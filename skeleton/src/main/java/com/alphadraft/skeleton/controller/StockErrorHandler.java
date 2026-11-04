package com.alphadraft.skeleton.controller;

import com.alphadraft.skeleton.validation.StockValidationException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Turns stock request problems into 400 responses. Only StockController's requests are covered.
@RestControllerAdvice(assignableTypes = StockController.class)
public class StockErrorHandler {

    // 400 with { "errors": { field: message } }, the shape the frontend's stockService reads.
    @ExceptionHandler(StockValidationException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleValidation(StockValidationException e) {
        return errors(e.getErrors());
    }

    // A missing or malformed JSON body gets the same 400 shape instead of Spring's default.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Map<String, String>>> handleUnreadableBody(HttpMessageNotReadableException e) {
        return errors(Map.of("body", "Request body must be a JSON stock with ticker, name and sector."));
    }

    private static ResponseEntity<Map<String, Map<String, String>>> errors(Map<String, String> errors) {
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }
}
