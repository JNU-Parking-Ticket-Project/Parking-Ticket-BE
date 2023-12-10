// package com.jnu.ticketapi.config.response;
//
//
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.jnu.ticketcommon.dto.SuccessResponse;
// import java.io.IOException;
// import javax.servlet.http.HttpServletResponse;
// import lombok.RequiredArgsConstructor;
// import org.springframework.core.MethodParameter;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import org.springframework.http.server.ServerHttpRequest;
// import org.springframework.http.server.ServerHttpResponse;
// import org.springframework.http.server.ServletServerHttpResponse;
// import org.springframework.web.bind.annotation.RestControllerAdvice;
// import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
//
// @RestControllerAdvice(basePackages = "com.jnu")
// @RequiredArgsConstructor
// public class SuccessResponseAdvice implements ResponseBodyAdvice {
//    private final ObjectMapper objectMapper;
//
//    @Override
//    public boolean supports(MethodParameter returnType, Class converterType) {
//        return true;
//    }
//
//    @Override
//    public Object beforeBodyWrite(
//            Object body,
//            MethodParameter returnType,
//            MediaType selectedContentType,
//            Class selectedConverterType,
//            ServerHttpRequest request,
//            ServerHttpResponse response) {
//        HttpServletResponse servletResponse =
//                ((ServletServerHttpResponse) response).getServletResponse();
//
//        int status = servletResponse.getStatus();
//        HttpStatus resolve = HttpStatus.resolve(status);
//
//        if (resolve == null || body instanceof String) {
//            // json 형태로 변환
//            try {
//                servletResponse.getWriter().write(objectMapper.writeValueAsString(body));
////                response.getBody().write(objectMapper.writeValueAsBytes(body));
//            } catch (IOException e) {
//                // 있어서는 안될일이다!
//                throw new RuntimeException(e);
//            }
//        }
//
//        if (resolve.is2xxSuccessful()) {
//            SuccessResponse successResponse = new SuccessResponse(status, body);
//            try {
////                // json 형태로 변환
//                servletResponse.getWriter().write(objectMapper.writeValueAsString(body));
//
//                response.getBody().write(objectMapper.writeValueAsBytes(successResponse));
//
//            } catch (IOException e) {
//                // 있어서는 안될일이다!
//                throw new RuntimeException(e);
//            }
//            return null;
//        }
//
//        return body;
//    }
// }
