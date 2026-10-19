package com.example.backend.service;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Клиент к {@code llm-service} (прокси к Ollama, порт 8081).
 * Запрашивает комментарий к ходу ИИ через {@code POST /api/chat} с streaming-ответом.
 * <p>При недоступности сервиса или таймауте вызывающий код должен перейти на fallback.
 */
public class LlmServiceClient {

    private static final String CHAT_PATH = "/api/chat";

    private final RestClient restClient;

    public LlmServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Запросить комментарий у LLM.
     *
     * @param prompt текст промпта
     * @param model  модель (должна быть в allowlist, проверяется вызывающей стороной)
     * @return текст комментария, или {@code null}, если сервер вернул пустой ответ
     * @throws LlmServiceUnavailable при недоступности/таймауте/ошибке llm-service
     */
    public String chat(String prompt, String model) {
        try {
            String response = restClient.post()
                    .uri(CHAT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .body(new ChatRequest(prompt, model))
                    .retrieve()
                    .body(String.class);
            if (response == null || response.isBlank()) {
                return null;
            }
            return parseSse(response);
        } catch (RestClientResponseException e) {
            throw new LlmServiceUnavailable(
                    "llm-service вернул ошибку HTTP " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new LlmServiceUnavailable("llm-service недоступен: " + e.getMessage(), e);
        }
    }

    /** Разобрать SSE-ответ: строки {@code data: текст} объединяются в один комментарий. */
    private static String parseSse(String response) {
        StringBuilder sb = new StringBuilder();
        for (String line : response.split("\n", -1)) {
            if (line.startsWith("data: ")) {
                sb.append(line.substring(6));
            } else if (!line.isBlank() && !line.startsWith(":")) {
                sb.append(line);
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    /** Тело запроса к llm-service /api/chat. */
    public record ChatRequest(String prompt, String model) {
    }

    /** llm-service недоступен (таймаут, ошибка сети, HTTP-ошибка). */
    public static class LlmServiceUnavailable extends RuntimeException {
        public LlmServiceUnavailable(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
