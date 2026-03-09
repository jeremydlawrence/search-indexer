package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.EmbeddingProperties;
import org.example.model.EmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final EmbeddingProperties configProperties;
    private final HttpClient httpClient;

    @Autowired
    public EmbeddingService(EmbeddingProperties configProperties) {
        this.configProperties = configProperties;
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<List<Float>> getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            logger.warn("Attempted to get embeddings for null or empty list");
            return List.of();
        }

        logger.debug("Getting embeddings for {} texts", texts.size());

        try {
            String requestBody = buildRequestBody(texts);
            HttpRequest request = buildRequest(requestBody);

            final long start = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("Generated {} embeddings in {}ms", texts.size(), System.currentTimeMillis() - start);

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                throw new RuntimeException("Failed to get embeddings: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            logger.error("Error getting embeddings for text: {}", texts, e);
            return List.of();
        }
    }

    protected String buildRequestBody(List<String> texts) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"texts\": [");
        for (int i = 0; i < texts.size(); i++) {
            sb.append("\"").append(sanitizeText(texts.get(i))).append("\"");
            if (i < texts.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    protected String sanitizeText(String text) {
        if (text == null) {
            return "";
        }

        String sanitized = text.replaceAll("\\\\{1}", "\\\\\\\\"); // escape solo backslashes
        sanitized = sanitized.replace("\"", "\\\""); // escape quotes
        sanitized = sanitized.replaceAll("[\\n\\t\\r]", " "); // remove control chars
        sanitized = sanitized.replaceAll("\\s+", " "); // collapse multiple spaces
        int charLimit = configProperties.getCharLimit();
        if (charLimit > 0 && sanitized.length() > charLimit) {
            sanitized = sanitized.substring(0, charLimit);
            logger.debug("Text truncated to {} characters", charLimit);
        }
        
        return sanitized;
    }

    protected HttpRequest buildRequest(String requestBody) {
        String url = String.format("%s://%s:%d/%s",
                configProperties.getProtocol(),
                configProperties.getHost(),
                configProperties.getPort(),
                configProperties.getPath());

        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    protected List<List<Float>> parseResponse(String responseBody) throws Exception {
        EmbeddingResponse embeddingResponse = objectMapper.readValue(responseBody, EmbeddingResponse.class);
        return embeddingResponse.getVectors();
    }
}
