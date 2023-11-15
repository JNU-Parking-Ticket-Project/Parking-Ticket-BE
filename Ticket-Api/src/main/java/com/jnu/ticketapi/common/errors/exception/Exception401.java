package com.jnu.ticketapi.common.errors.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import pickup_shuttle.pickup._core.utils.ApiUtils;

@Getter
public class Exception401 extends RuntimeException{
    public Exception401(String message) { super(message); }

    public ApiUtils.ApiResult<?> body(){ return ApiUtils.error(getMessage(), HttpStatus.UNAUTHORIZED); }

    public HttpStatus status(){
        return HttpStatus.UNAUTHORIZED;
    }
}
