package com.aipaperreview.service.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LlmJsonPayloadExtractor {
    private static final Pattern FENCED_JSON = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private LlmJsonPayloadExtractor() {
    }

    static Optional<JsonNode> extract(ObjectMapper objectMapper, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.strip();
        Optional<JsonNode> complete = extractComplete(objectMapper, trimmed);
        if (complete.isPresent()) {
            return complete;
        }

        return extractBalancedJson(objectMapper, trimmed);
    }

    static Optional<JsonNode> extractComplete(ObjectMapper objectMapper, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.strip();
        Optional<JsonNode> direct = tryParse(objectMapper, trimmed);
        if (direct.isPresent()) {
            return direct;
        }

        Matcher matcher = FENCED_JSON.matcher(trimmed);
        while (matcher.find()) {
            Optional<JsonNode> fenced = tryParse(objectMapper, matcher.group(1).strip());
            if (fenced.isPresent()) {
                return fenced;
            }
        }
        return Optional.empty();
    }

    static String normalize(ObjectMapper objectMapper, String value) {
        return extract(objectMapper, value)
                .map(JsonNode::toString)
                .orElse(value);
    }

    private static Optional<JsonNode> tryParse(ObjectMapper objectMapper, String value) {
        try {
            return Optional.of(objectMapper.readTree(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Optional<JsonNode> extractBalancedJson(ObjectMapper objectMapper, String value) {
        for (int start = 0; start < value.length(); start++) {
            char first = value.charAt(start);
            if (first != '{' && first != '[') {
                continue;
            }
            Optional<String> candidate = balancedCandidate(value, start);
            if (candidate.isEmpty()) {
                if (first == '[') {
                    return Optional.empty();
                }
                continue;
            }
            Optional<JsonNode> parsed = tryParse(objectMapper, candidate.get());
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> balancedCandidate(String value, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            } else if (ch == '{' || ch == '[') {
                depth++;
            } else if (ch == '}' || ch == ']') {
                depth--;
                if (depth == 0) {
                    return Optional.of(value.substring(start, index + 1));
                }
            }
        }
        return Optional.empty();
    }
}
