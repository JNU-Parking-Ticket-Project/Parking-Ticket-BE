package com.jnu.ticketapi.common.errors.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.jnu.ticketapi.common.utils.ApiUtils;


// 유효성 검사 실패, 잘못된 파라메터 요청
@Getter
public class Exception400 extends RuntimeException {

    public Exception400(String message) {
        super(message);
    }

    public ApiUtils.ApiResult<?> body(){
        return ApiUtils.error(getMessage(), HttpStatus.BAD_REQUEST);
    }

    public HttpStatus status(){
        return HttpStatus.BAD_REQUEST;
    }
}