package com.gdgoc5.vitaltrip.exception;

import com.gdgoc5.vitaltrip.custom.ErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleEnumTypeMismatch(MethodArgumentTypeMismatchException e) {
        if (e.getRequiredType() != null && e.getRequiredType().isEnum()) {
            return ResponseEntity
                    .status(404)
                    .body(ErrorResponse.of("해당 응급처치 유형은 존재하지 않습니다: " + e.getValue(), "NOT_FOUND"));
        }
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of("잘못된 요청입니다.", "BAD_REQUEST"));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e) {
        return ResponseEntity
                .status(404)
                .body(ErrorResponse.of(e.getMessage(), "NOT_FOUND"));
    }
}
