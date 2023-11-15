package com.jnu.ticketapi.common.errors.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import pickup_shuttle.pickup._core.utils.ApiUtils;

@Getter
public class Exception403 extends RuntimeException{
    public Exception403(String message) {
        super(message);
    }

    public ApiUtils.ApiResult<?> body(){
        return ApiUtils.error(getMessage(), HttpStatus.FORBIDDEN);
    }

    public HttpStatus status(){
        return HttpStatus.FORBIDDEN;
    }
}
