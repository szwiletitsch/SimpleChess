package com.my.chessengine.api.exceptionHandlers;

import com.my.chessengine.api.Exceptions.ChessEngineIllegalArgument;
import com.my.chessengine.api.Exceptions.ChessEngineTimeout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class ChessExceptionHandler {
    @ExceptionHandler(ChessEngineIllegalArgument.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ChessEngineIllegalArgument ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 400,
                        "error", "Illegal argument",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(ChessEngineTimeout.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ChessEngineTimeout ex) {

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", 408,
                        "error", "Request timeout",
                        "message", ex.getMessage()
                ));
    }
}
