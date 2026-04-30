package com.cts.customerservices.payload;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private String status;

    private String message;

    private T data;

    private String errorCode;

    private String path;

    private LocalDateTime timestamp;

}
