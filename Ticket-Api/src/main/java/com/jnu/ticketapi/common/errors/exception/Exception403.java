package com.jnu.ticketapi.common.errors.exception;


import com.jnu.ticketapi.common.utils.ApiResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class Exception403 extends RuntimeException {
    public Exception403(String message) {
        super(message);
    }

    public ApiResponse.ApiResult<?> body() {
        return ApiResponse.error(getMessage(), HttpStatus.FORBIDDEN);
    }

    public HttpStatus status() {
        return HttpStatus.FORBIDDEN;
    }
}
