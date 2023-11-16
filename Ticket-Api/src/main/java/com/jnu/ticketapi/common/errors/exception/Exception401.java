package com.jnu.ticketapi.common.errors.exception;


import com.jnu.ticketapi.common.utils.ApiResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class Exception401 extends RuntimeException {
    public Exception401(String message) {
        super(message);
    }

    public ApiResponse.ApiResult<?> body() {
        return ApiResponse.error(getMessage(), HttpStatus.UNAUTHORIZED);
    }

    public HttpStatus status() {
        return HttpStatus.UNAUTHORIZED;
    }
}
