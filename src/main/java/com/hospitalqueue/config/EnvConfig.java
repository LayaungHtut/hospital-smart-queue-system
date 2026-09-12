package com.hospitalqueue.config;

import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the project-level .env file (Neon + OpenRouter credentials)
 * so that the values can be referenced by other configuration and services.
 * Falls back to OS environment variables when a key is not in the .env file.
 */
@Configuration
public class EnvConfig {

    private final Map<String, String> values = new HashMap<>();

    public EnvConfig() {
        load();
    }

    private void load() {
        List<Path> candidates = List.of(
                Paths.get(".env"),
                Paths.get(System.getProperty("user.dir", "."), ".env"));
        for (Path path : candidates) {
            if (Files.exists(path)) {
                try {
                    List<String> lines = Files.readAllLines(path);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int idx = trimmed.indexOf('=');
                        if (idx <= 0) {
                            continue;
                        }
                        String key = trimmed.substring(0, idx).trim();
                        String value = trimmed.substring(idx + 1).trim();
                        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }
                        if (!key.isEmpty()) {
                            values.put(key, value);
                        }
                    }
                } catch (IOException e) {
                    // ignore, fall back to environment variables
                }
                break;
            }
        }
    }

    public String get(String key) {
        String value = values.get(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return System.getenv(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    public boolean getBool(String key, boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }
}
