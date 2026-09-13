package com.hospitalqueue.ml;

import com.hospitalqueue.ai.OpenRouterClient;
import com.hospitalqueue.config.EnvConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Retrieval-augmented hospital FAQ chatbot.
 *
 * "Retrieval" = keyword-scored lookup over the plain-text files in
 * {@code src/main/resources/knowledge-base/*.txt} (loaded once at startup -
 * add/edit .txt files there and restart to update what the bot knows).
 * "Generation" = a call to {@link OpenRouterClient}, the same free-tier
 * OpenRouter client the rest of the AI features use, so this bot gets its
 * circuit breaker, response cache and primary/fallback model retry for free
 * instead of keeping a second, independently-configured LLM client around.
 */
@Service
public class RagChatbotService {

    private static final Logger log = LoggerFactory.getLogger(RagChatbotService.class);

    private final OpenRouterClient openRouterClient;
    private final String knowledgeBasePath;

    private List<KbChunk> chunks = new ArrayList<>();
    private boolean initialized = false;
    private final Map<String, List<String>> sessionHistory = new ConcurrentHashMap<>();

    public RagChatbotService(OpenRouterClient openRouterClient, EnvConfig env) {
        this.openRouterClient = openRouterClient;
        this.knowledgeBasePath = env.getOrDefault("KNOWLEDGE_BASE_PATH", "classpath:knowledge-base/*.txt");
    }

    @PostConstruct
    public void init() {
        if (!openRouterClient.isConfigured()) {
            log.warn("Chatbot disabled: OpenRouter API key not configured");
            return;
        }
        loadKnowledgeBase();
        initialized = true;
        log.info("RAG chatbot initialized successfully with {} chunks", chunks.size());
    }

    private void loadKnowledgeBase() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:knowledge-base/*.txt");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String content = reader.lines().collect(Collectors.joining("\n"));
                    List<String> paragraphs = Arrays.stream(content.split("\n\n+"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();

                    for (String paragraph : paragraphs) {
                        chunks.add(new KbChunk(filename, paragraph));
                    }
                }
            }

            if (chunks.isEmpty()) {
                log.warn("No .txt files found in knowledge-base, using built-in defaults");
                loadDefaultKnowledge();
            } else {
                log.info("Loaded {} chunks from {} files", chunks.size(), resources.length);
            }
        } catch (Exception e) {
            log.error("Failed to load knowledge base: {}", e.getMessage());
            loadDefaultKnowledge();
        }
    }

    private void loadDefaultKnowledge() {
        chunks.add(new KbChunk("hospital_info.txt", """
                Hospital General Information:
                - Name: City General Hospital
                - Address: 123 Medical Center Drive, Health City, HC 12345
                - Phone: (555) 123-4567
                - Emergency: (555) 123-9111
                - Hours: 24/7 Emergency, Outpatient 8AM-8PM Mon-Sat
                """));
        chunks.add(new KbChunk("departments.txt", """
                Departments and Locations:
                - Cardiology (CAR): Building A, 2nd Floor
                - Neurology (NEU): Building A, 3rd Floor
                - Orthopedics (ORT): Building B, 1st Floor
                - General Medicine (GEN): Building A, 1st Floor
                - Pediatrics (PED): Building C, 1st Floor
                - Dermatology (DER): Building B, 2nd Floor
                """));
        chunks.add(new KbChunk("emergency.txt", """
                Emergency Department:
                - Location: Building A, Ground Floor (separate entrance)
                - Triage: All patients assessed within 10 minutes of arrival
                - Levels: 1=Resuscitation, 2=Emergent, 3=Urgent, 4=Less Urgent, 5=Non-Urgent
                """));
    }

    public String chat(String userMessage) {
        if (!initialized) {
            return "Chatbot is not available. Please configure the OpenRouter API key.";
        }
        String context = retrieveRelevantContext(userMessage);
        String systemPrompt = """
                You are a helpful hospital assistant for City General Hospital.
                Answer questions based ONLY on the provided context.
                If the context doesn't contain enough information, say so politely.
                Keep answers concise and friendly.

                CONTEXT:
                %s
                """.formatted(context);

        String response = openRouterClient.chat(systemPrompt, userMessage);
        return response != null ? response : "I'm having trouble processing your request. Please try again.";
    }

    public String chatWithSession(String sessionId, String userMessage) {
        if (!initialized) {
            return "Chatbot is not available.";
        }
        List<String> history = sessionHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        String context = retrieveRelevantContext(userMessage);

        StringBuilder conversationContext = new StringBuilder();
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size(); i++) {
            conversationContext.append(history.get(i)).append("\n");
        }

        String systemPrompt = """
                You are a helpful hospital assistant for City General Hospital.
                Answer questions based ONLY on the provided context and conversation history.
                If the context doesn't contain enough information, say so politely.
                Keep answers concise and friendly.

                HOSPITAL KNOWLEDGE:
                %s

                RECENT CONVERSATION:
                %s
                """.formatted(context, conversationContext);

        String response = openRouterClient.chat(systemPrompt, userMessage);
        if (response == null) {
            return "I'm having trouble processing your request.";
        }
        history.add("User: " + userMessage);
        history.add("Assistant: " + response);
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }
        return response;
    }

    public boolean isReady() {
        return initialized;
    }

    public void addDocument(String content, String metadata) {
        if (!initialized) return;
        chunks.add(new KbChunk(metadata != null ? metadata : "custom.txt", content));
        log.info("Added custom document chunk: {} chars", content.length());
    }

    private String retrieveRelevantContext(String query) {
        if (chunks.isEmpty()) return "No hospital knowledge available.";

        String queryLower = query.toLowerCase();
        String[] queryWords = queryLower.split("\\W+");

        List<ScoredChunk> scored = chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, scoreChunk(queryWords, chunk)))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(3)
                .toList();

        return scored.stream()
                .filter(sc -> sc.score() > 0)
                .map(sc -> sc.chunk().content())
                .collect(Collectors.joining("\n\n"));
    }

    private double scoreChunk(String[] queryWords, KbChunk chunk) {
        String contentLower = chunk.content().toLowerCase();
        double score = 0;
        for (String word : queryWords) {
            if (word.length() < 2) continue;
            if (contentLower.contains(word)) {
                score += 1.0;
            }
        }
        return score;
    }

    public ChatbotStatus getStatus() {
        return new ChatbotStatus(
                initialized,
                openRouterClient.getModel(),
                "In-memory file RAG (" + knowledgeBasePath + ")",
                chunks.size()
        );
    }

    private record KbChunk(String source, String content) {}
    private record ScoredChunk(KbChunk chunk, double score) {}
    public record ChatbotStatus(boolean ready, String model, String embeddingStore, int documentCount) {}
}
