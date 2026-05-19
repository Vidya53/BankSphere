package com.cts.loanservice.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Hidden
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private int statusCode;
    private String message;
    private T data;
    private Object errors;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Created successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> failure(String message, Object errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> failure(HttpStatus status, String message, Object errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(status.value())
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
