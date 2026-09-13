package com.hospitalqueue.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalqueue.config.EnvConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thin client for the OpenRouter API used to run free LLM models.
 * Credentials and model names come from the .env file:
 *   OPENROUTER_API_KEY, OPENROUTER_MODEL, OPENROUTER_BASE_URL
 *
 * Optimizations over baseline:
 * - HTTP/2 for better connection reuse
 * - 25-second request timeout
 * - 500 max_tokens - free-tier "thinking" models spend a chunk of this on a
 *   hidden reasoning pass (billed against max_tokens even when
 *   reasoning.exclude drops it from the response), so a small budget makes
 *   them hit the length limit before writing any visible content
 * - SHA-256 response cache with 5-minute TTL
 * - Circuit breaker: after 3 consecutive failures, skip AI for 30 seconds
 */
@Component
public class OpenRouterClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);

    private static final int REQUEST_TIMEOUT_SECONDS = 25;
    private static final int MAX_TOKENS = 500;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5 minutes
    private static final int CIRCUIT_BREAKER_THRESHOLD = 3;
    private static final long CIRCUIT_BREAKER_COOLDOWN_MS = 30 * 1000L; // 30 seconds

    private final EnvConfig env;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Response cache: SHA-256(prompt) -> CacheEntry
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // Circuit breaker state
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<Instant> circuitOpenUntil = new AtomicReference<>(Instant.MIN);

    public OpenRouterClient(EnvConfig env) {
        this.env = env;
    }

    /**
     * The primary model this client currently calls (for status/diagnostics
     * display - the actual call may fall back to OPENROUTER_FALLBACK_MODEL).
     */
    public String getModel() {
        return env.getOrDefault("OPENROUTER_MODEL", "nex-agi/nex-n2.5-pro:free");
    }

    /**
     * Returns true when an API key is configured (AI features enabled).
     */
    public boolean isConfigured() {
        String key = env.get("OPENROUTER_API_KEY");
        return key != null && !key.isBlank() && !key.contains("YOUR_OPENROUTER_API_KEY");
    }

    /**
     * Runs a chat completion against OpenRouter and returns the assistant text.
     * Returns null when the call fails or AI is not configured.
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            log.warn("OpenRouter is not configured - skipping AI call.");
            return null;
        }

        // Circuit breaker: if open, fail fast
        Instant openUntil = circuitOpenUntil.get();
        if (Instant.now().isBefore(openUntil)) {
            log.warn("Circuit breaker open - skipping AI call for {}s",
                    java.time.temporal.ChronoUnit.SECONDS.between(Instant.now(), openUntil));
            return null;
        }

        // Check cache
        String cacheKey = sha256(systemPrompt + "|" + userPrompt);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && Instant.now().isBefore(cached.expiresAt)) {
            log.debug("Cache hit for prompt (key={})", cacheKey.substring(0, 8));
            return cached.response;
        }

        String baseUrl = env.getOrDefault("OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1");
        String model = env.getOrDefault("OPENROUTER_MODEL", "nex-agi/nex-n2.5-pro:free");
        String fallbackModel = env.getOrDefault("OPENROUTER_FALLBACK_MODEL", "cohere/north-mini-code:free");
        String apiKey = env.get("OPENROUTER_API_KEY");

        String result = callModel(baseUrl, model, apiKey, systemPrompt, userPrompt);
        if (result == null && fallbackModel != null && !fallbackModel.isBlank() && !fallbackModel.equals(model)) {
            log.warn("Primary model '{}' failed, retrying with fallback '{}'", model, fallbackModel);
            result = callModel(baseUrl, fallbackModel, apiKey, systemPrompt, userPrompt);
        }

        if (result == null) {
            recordFailure();
            return null;
        }

        // Cache the successful response
        cache.put(cacheKey, new CacheEntry(result, Instant.now().plusMillis(CACHE_TTL_MS)));
        failureCount.set(0);
        return result;
    }

    /**
     * Runs one chat completion call against a specific model. Returns null on
     * any failure (non-200 response, missing/blank content) so the caller can
     * decide whether to retry with a fallback model.
     *
     * {@code reasoning.exclude} is set because free-tier OpenRouter models are
     * frequently "thinking" models that spend the whole token budget on an
     * internal <reasoning> trace and leave "content" null/empty otherwise.
     */
    private String callModel(String baseUrl, String model, String apiKey, String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            payload.put("temperature", 0.2);
            payload.put("max_tokens", MAX_TOKENS);
            payload.put("reasoning", Map.of("exclude", true));

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "https://localhost:8080")
                    .header("X-Title", "Hospital Smart Queue System")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("OpenRouter ({}) returned status {}: {}", model, response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String text = content.isMissingNode() || content.isNull() ? "" : content.asText().trim();
            if (text.isEmpty()) {
                log.warn("OpenRouter ({}) returned empty content: {}", model, response.body());
                return null;
            }
            return text;
        } catch (Exception e) {
            log.warn("OpenRouter call to {} failed: {}", model, e.getMessage());
            return null;
        }
    }

    private void recordFailure() {
        int failures = failureCount.incrementAndGet();
        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            circuitOpenUntil.set(Instant.now().plusMillis(CIRCUIT_BREAKER_COOLDOWN_MS));
            log.warn("Circuit breaker OPEN after {} consecutive failures - AI calls paused for 30s",
                    failures);
            failureCount.set(0);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on all JVMs
            return String.valueOf(input.hashCode());
        }
    }

    private record CacheEntry(String response, Instant expiresAt) {}
}
