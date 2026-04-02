package com.example.demo.service;

import com.example.demo.vo.RestObject;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RestObject<Map<String, Object>>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(RestObject.newFail(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), buildData()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestObject<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().isEmpty()
            ? "invalid request"
            : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(RestObject.newFail(HttpStatus.BAD_REQUEST.value(), message, buildData()));
    }

    private Map<String, Object> buildData() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }
}
