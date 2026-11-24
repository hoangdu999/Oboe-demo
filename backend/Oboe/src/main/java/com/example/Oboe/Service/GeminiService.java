package com.example.Oboe.Service;

import com.example.Oboe.DTOs.QuestionDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class GeminiService {
    private static final int MAX_RETRIES = 3;
    private static final List<Integer> RETRYABLE_STATUS_CODES = List.of(429, 503);

    // Danh sách model dự phòng theo thứ tự ưu tiên.
    // Khi model chính trả về 503 (quá tải), sẽ thử lần lượt các model này.
    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-3.1-flash-lite",
            "gemini-3.8-flash-lite-tts",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash"
    );

    @Value("${gemini.api.url:}")
    private String geminiApiUrl;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-3.8-flash}")
    private String geminiModel;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiBaseUrl;

    // Hàm cũ: generate list câu hỏi dạng DTO
    public List<QuestionDTO> generateQuestion(String prompt) {
        RestTemplate restTemplate = createRestTemplate();

        Map<String, Object> textMap = new HashMap<>();
        textMap.put("text", prompt);

        Map<String, Object> partMap = new HashMap<>();
        partMap.put("parts", List.of(textMap));
        partMap.put("role", "user");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(partMap));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = callGeminiApi(restTemplate, entity);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                if (body.containsKey("candidates")) {
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                    if (!candidates.isEmpty()) {
                        Map<String, Object> firstCandidate = candidates.get(0);
                        if (firstCandidate.containsKey("content")) {
                            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

                            if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                                String rawResponse = parts.get(0).get("text").toString().trim();

                                // Cắt bỏ phần ngoài nếu cần
                                int jsonStart = rawResponse.indexOf("[");
                                int jsonEnd = rawResponse.lastIndexOf("]");
                                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                                    rawResponse = rawResponse.substring(jsonStart, jsonEnd + 1);
                                }

                                ObjectMapper mapper = new ObjectMapper();

                                List<Map<String, Object>> rawList = mapper.readValue(rawResponse,
                                        new TypeReference<List<Map<String, Object>>>() {});
                                List<QuestionDTO> dtoList = new ArrayList<>();

                                for (Map<String, Object> item : rawList) {
                                    QuestionDTO dto = new QuestionDTO();
                                    dto.setQuestionID(UUID.randomUUID());
                                    dto.setQuestionName((String) item.get("question"));
                                    dto.setCorrectAnswer((String) item.get("answer"));

                                    Object choicesObj = item.get("choices");
                                    if (choicesObj instanceof List) {
                                        dto.setOptions((List<String>) choicesObj);
                                    }

                                    dtoList.add(dto);
                                }

                                return dtoList;
                            }
                        }
                    }
                }
                throw new RuntimeException("Không có nội dung hợp lệ từ Gemini.");
            } else {
                throw new RuntimeException("Lỗi HTTP: " + response.getStatusCode());
            }

        } catch (HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            throw new RuntimeException("Lỗi gọi Gemini API (" + ex.getStatusCode().value() + "): " + responseBody);
        } catch (Exception ex) {
            throw new RuntimeException("Lỗi gọi Gemini API: " + ex.getMessage());
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Hàm mới: gửi prompt và trả về nguyên văn text (chấm điểm, nhận xét,...)
    public String generateTextFromPrompt(String prompt) {
        RestTemplate restTemplate = createRestTemplate();

        Map<String, Object> textMap = new HashMap<>();
        textMap.put("text", prompt);

        Map<String, Object> partMap = new HashMap<>();
        partMap.put("parts", List.of(textMap));
        partMap.put("role", "user");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(partMap));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = callGeminiApi(restTemplate, entity);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                if (body.containsKey("candidates")) {
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                    if (!candidates.isEmpty()) {
                        Map<String, Object> firstCandidate = candidates.get(0);
                        if (firstCandidate.containsKey("content")) {
                            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

                            if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                                String rawText = parts.get(0).get("text").toString().trim();

                                // ======= BỎ CÁC KÝ HIỆU CODE BLOCK ` ```json ... ``` ` =======
                                if (rawText.startsWith("```json")) {
                                    rawText = rawText.substring(7).trim(); // bỏ "```json"
                                } else if (rawText.startsWith("```")) {
                                    rawText = rawText.substring(3).trim(); // bỏ "```"
                                }

                                if (rawText.endsWith("```")) {
                                    rawText = rawText.substring(0, rawText.length() - 3).trim(); // bỏ ``` ở cuối
                                }

                                return rawText;
                            }
                        }
                    }
                }

                throw new RuntimeException("Không có nội dung hợp lệ từ Gemini.");
            } else {
                throw new RuntimeException("Lỗi HTTP: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException ex) {
            String responseBody = ex.getResponseBodyAsString();
            throw new RuntimeException("Lỗi gọi Gemini API (" + ex.getStatusCode().value() + "): " + responseBody);
        } catch (Exception ex) {
            throw new RuntimeException("Lỗi gọi Gemini API: " + ex.getMessage());
        }
    }

    public JsonNode parseJsonResponse(String jsonResponse) {
        try {
            return objectMapper.readTree(jsonResponse);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi phân tích JSON từ Gemini: " + e.getMessage());
        }
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(110000);
        return new RestTemplate(factory);
    }

    private String resolveGeminiApiUrl() {
        if (geminiApiUrl != null && !geminiApiUrl.isBlank() && !geminiApiUrl.contains("gemini-not-configured")) {
            return geminiApiUrl.trim();
        }
        return buildModelUrl(geminiModel);
    }

    private String buildModelUrl(String model) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new RuntimeException("Gemini API chưa được cấu hình. Hãy thêm GEMINI_API_KEY vào file backend/Oboe/.env.local.");
        }

        String baseUrl = geminiApiBaseUrl == null || geminiApiBaseUrl.isBlank()
                ? "https://generativelanguage.googleapis.com/v1beta/models"
                : geminiApiBaseUrl.trim().replaceAll("/+$", "");
        String modelName = model == null || model.isBlank()
                ? "gemini-3.8-flash"
                : model.trim();

        return baseUrl + "/" + modelName + ":generateContent?key="
                + URLEncoder.encode(geminiApiKey.trim(), StandardCharsets.UTF_8);
    }

    /**
     * Gọi Gemini API với cơ chế fallback model.
     * - Thử model chính trước, nếu trả về 503 (quá tải) thì thử lần lượt các model dự phòng.
     * - Mỗi model được retry tối đa MAX_RETRIES lần đối với lỗi 429 (rate limit).
     */
    private ResponseEntity<Map> callGeminiApi(RestTemplate restTemplate, HttpEntity<Map<String, Object>> entity) {
        // Nếu user đã override gemini.api.url, chỉ dùng URL đó (không fallback model)
        if (geminiApiUrl != null && !geminiApiUrl.isBlank() && !geminiApiUrl.contains("gemini-not-configured")) {
            return callWithRetries(restTemplate, entity, geminiApiUrl.trim());
        }

        // Xây dựng danh sách model candidate: model chính + fallback, loại trùng
        List<String> candidateModels = new ArrayList<>();
        candidateModels.add(geminiModel);
        for (String fallback : FALLBACK_MODELS) {
            if (!candidateModels.contains(fallback)) {
                candidateModels.add(fallback);
            }
        }

        RuntimeException lastException = null;

        for (String model : candidateModels) {
            String modelUrl = buildModelUrl(model);
            try {
                return callWithRetries(restTemplate, entity, modelUrl);
            } catch (RuntimeException ex) {
                lastException = ex;
                // Nếu lỗi do 503 (quá tải), tiếp tục thử model tiếp theo
                if (ex.getMessage() != null && ex.getMessage().contains("(503)")) {
                    continue;
                }
                // Các lỗi khác (400, 401, 404, 429 quá nhiều lần,...) throw ngay
                throw ex;
            }
        }

        throw new RuntimeException("Tất cả model Gemini đều quá tải. Vui lòng thử lại sau. "
                + (lastException != null ? lastException.getMessage() : ""));
    }

    private ResponseEntity<Map> callWithRetries(RestTemplate restTemplate, HttpEntity<Map<String, Object>> entity, String url) {
        HttpStatusCodeException lastRetryableException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            } catch (HttpStatusCodeException ex) {
                int statusCode = ex.getStatusCode().value();
                if (RETRYABLE_STATUS_CODES.contains(statusCode) && attempt < MAX_RETRIES) {
                    lastRetryableException = ex;
                    sleepBeforeRetry(attempt);
                    continue;
                }

                String responseBody = ex.getResponseBodyAsString();
                throw new RuntimeException("Lỗi gọi Gemini API (" + statusCode + "): " + responseBody);
            }
        }

        if (lastRetryableException != null) {
            throw new RuntimeException("Lỗi gọi Gemini API (" + lastRetryableException.getStatusCode().value() + "): "
                    + lastRetryableException.getResponseBodyAsString());
        }

        throw new RuntimeException("Lỗi gọi Gemini API: không nhận được phản hồi hợp lệ.");
    }

    private void sleepBeforeRetry(int attempt) {
        long delayMs = switch (attempt) {
            case 1 -> 1500L;
            case 2 -> 3000L;
            default -> 4500L;
        };

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Tiến trình gọi Gemini bị gián đoạn.");
        }
    }
}
