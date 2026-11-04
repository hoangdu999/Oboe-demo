package com.example.Oboe.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 403,
                        "error", ex.getMessage()
                )
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        HttpStatus status = resolveStatus(ex.getMessage());
        return ResponseEntity.status(status).body(
                Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", status.value(),
                        "error", ex.getMessage() == null ? "Lỗi hệ thống." : ex.getMessage()
                )
        );
    }

    private HttpStatus resolveStatus(String message) {
        if (message == null || message.isBlank()) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Matcher matcher = Pattern.compile("Lỗi gọi Gemini API \\((\\d{3})\\)").matcher(message);
        if (matcher.find()) {
            try {
                return HttpStatus.valueOf(Integer.parseInt(matcher.group(1)));
            } catch (IllegalArgumentException ignored) {
                return HttpStatus.BAD_GATEWAY;
            }
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
