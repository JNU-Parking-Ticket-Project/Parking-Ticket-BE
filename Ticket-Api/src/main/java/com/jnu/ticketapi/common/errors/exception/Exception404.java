package com.jnu.ticketapi.common.errors.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import pickup_shuttle.pickup._core.utils.ApiUtils;

@Getter
public class Exception404 extends RuntimeException {
    public Exception404(String message) {
        super(message);
    }

    public ApiUtils.ApiResult<?> body(){
        return ApiUtils.error(getMessage(), HttpStatus.NOT_FOUND);
    }

    public HttpStatus status(){
        return HttpStatus.NOT_FOUND;
    }
}
