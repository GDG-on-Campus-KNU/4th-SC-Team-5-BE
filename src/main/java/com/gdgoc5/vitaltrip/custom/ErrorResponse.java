package com.gdgoc5.vitaltrip.custom;

public record ErrorResponse(
        String result,
        String message,
        String code
) {
    public static ErrorResponse of(String message, String code) {
        return new ErrorResponse("FAIL", message, code);
    }
}
