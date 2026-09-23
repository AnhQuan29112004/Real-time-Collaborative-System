package com.collabnote.common.response;

public record ApiResponse<T>(int code, String message, T data, String errorCode, String requestId) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data, null, null);
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static ApiResponse<Void> error(int status, String errorCode, String message, String requestId) {
        return new ApiResponse<>(status, message, null, errorCode, requestId);
    }
}
