package com.jnu.ticketapi.common.errors.exception;


import com.jnu.ticketapi.common.utils.ApiResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class Exception404 extends RuntimeException {
    public Exception404(String message) {
        super(message);
    }

    public ApiResponse.ApiResult<?> body() {
        return ApiResponse.error(getMessage(), HttpStatus.NOT_FOUND);
    }

    public HttpStatus status() {
        return HttpStatus.NOT_FOUND;
    }
}
