package com.gdgoc5.vitaltrip.custom;

public record SuccessResponse<T>(
        String result,
        String message,
        T data
) {

    public static <T> SuccessResponse<T> of(T data) {
        return of(null, data);
    }

    public static <T> SuccessResponse<T> of(String message, T data) {
        if (message == null || message.isBlank()) {
            message = "요청이 성공적으로 처리되었습니다.";
        }
        return new SuccessResponse<>("SUCCESS", message, data);
    }
}
