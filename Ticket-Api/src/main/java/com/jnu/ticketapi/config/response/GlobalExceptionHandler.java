package com.jnu.ticketapi.config.response;

import static com.jnu.ticketcommon.consts.TicketStatic.*;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.jnu.ticketapi.api.slack.sender.SlackInternalErrorSender;
import com.jnu.ticketapi.config.SecurityUtils;
import com.jnu.ticketcommon.exception.*;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private final Environment environment;

    @Autowired(required = false)
    private SlackInternalErrorSender slackInternalErrorSender;

    /** Json 날짜 형식 파싱에 대한 에러 핸들러 내부에서 변환할 때 발생하는 에러입니다. */
    @ExceptionHandler({InvalidFormatException.class, DateTimeParseException.class})
    public ResponseEntity<ErrorResponse> jsonParseExceptionHandler(
            DateTimeParseException e, HttpServletRequest request) {
        String errorMessage;
        if (e.getMessage().contains("LocalDateTime")) {
            errorMessage = "날짜 형식이 올바르지 않습니다";
        } else {
            errorMessage = "Json 입력 형식 에러입니다";
        }
        ErrorReason errorReason =
                ErrorReason.builder()
                        .code("BAD_REQUEST")
                        .status(BAD_REQUEST)
                        .reason(errorMessage)
                        .build();
        ErrorResponse errorResponse =
                new ErrorResponse(errorReason, request.getRequestURL().toString());
        log.info("DateTimeParseException", e);
        return ResponseEntity.status(HttpStatus.valueOf(errorReason.getStatus()))
                .body(errorResponse);
    }

    @NotNull
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NotNull HttpHeaders headers,
            @NotNull HttpStatus status,
            @NotNull WebRequest request) {

        List<FieldError> errors = ex.getBindingResult().getFieldErrors();
        String errorsToJsonString;
        // HttpServletRequest Caching
        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        String url =
                UriComponentsBuilder.fromHttpRequest(
                                new ServletServerHttpRequest(servletWebRequest.getRequest()))
                        .build()
                        .toUriString();

        Map<String, Object> fieldAndErrorMessages =
                errors.stream()
                        .collect(
                                Collectors.toMap(
                                        FieldError::getField,
                                        fieldError ->
                                                Optional.ofNullable(fieldError.getDefaultMessage())
                                                        .orElse("메시지 없음")));

        try {
            errorsToJsonString =
                    fieldAndErrorMessages.values().stream()
                            .map(Object::toString)
                            .findFirst()
                            .orElse("메시지 없음");
        } catch (Exception e) {
            log.info("handleMethodArgumentNotValid: {}", e.getMessage(), e);
            throw JsonSerializeFailedException.EXCEPTION;
        }

        ErrorResponse errorResponse =
                new ErrorResponse(status.value(), status.name(), errorsToJsonString, url);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(TicketCodeException.class)
    public ResponseEntity<ErrorResponse> ticketCodeExceptionHandler(
            TicketCodeException e, HttpServletRequest request) {
        BaseErrorCode code = e.getErrorCode();
        ErrorReason errorReason = code.getErrorReason();
        log.info("TicketCodeException", e);
        ErrorResponse errorResponse =
                new ErrorResponse(errorReason, request.getRequestURL().toString());
        return ResponseEntity.status(HttpStatus.valueOf(errorReason.getStatus()))
                .body(errorResponse);
    }

    /** Request Param Validation 예외 처리 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolationExceptionHandler(
            ConstraintViolationException e, HttpServletRequest request) {
        Map<String, Object> bindingErrors = new HashMap<>();
        e.getConstraintViolations()
                .forEach(
                        constraintViolation -> {
                            List<String> propertyPath =
                                    List.of(
                                            constraintViolation
                                                    .getPropertyPath()
                                                    .toString()
                                                    .split(CONSTRAINT_VIOLATION_SEPARATOR));
                            String path =
                                    propertyPath.stream()
                                            .skip(propertyPath.size() - 1L)
                                            .findFirst()
                                            .orElse(null);
                            bindingErrors.put(path, constraintViolation.getMessage());
                        });

        ErrorReason errorReason =
                ErrorReason.builder()
                        .code("BAD_REQUEST")
                        .status(400)
                        .reason(bindingErrors.toString())
                        .build();
        ErrorResponse errorResponse =
                new ErrorResponse(errorReason, request.getRequestURL().toString());
        log.info("ConstraintViolationException", e);
        return ResponseEntity.status(HttpStatus.valueOf(errorReason.getStatus()))
                .body(errorResponse);
    }

    @ExceptionHandler(TicketDynamicException.class)
    public ResponseEntity<ErrorResponse> ticketDynamicExceptionHandler(
            TicketDynamicException e, HttpServletRequest request) {
        ErrorResponse errorResponse =
                new ErrorResponse(
                        e.getStatus(),
                        e.getCode(),
                        e.getReason(),
                        request.getRequestURL().toString());
        return ResponseEntity.status(HttpStatus.valueOf(e.getStatus())).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgumentExceptionHandler(
            IllegalArgumentException e, HttpServletRequest request) {
        ErrorResponse errorResponse =
                new ErrorResponse(
                        BAD_REQUEST,
                        BAD_REQUEST_CODE,
                        e.getMessage(),
                        request.getRequestURL().toString());
        log.info("IllegalArgumentException", e);
        return ResponseEntity.status(HttpStatus.valueOf(BAD_REQUEST)).body(errorResponse);
    }

    @ExceptionHandler(MultiException.class)
    protected ResponseEntity<ErrorResponse> multoExceptionHandler(
            MultiException e, HttpServletRequest request) {
        String url =
                UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                        .build()
                        .toUriString();
        String errorMessage =
                e.getCauses()
                        .map(TicketCodeException::getErrorReason)
                        .map(ErrorReason::getReason)
                        .collect(Collectors.joining("\\n"));
        GlobalErrorCode multiException = GlobalErrorCode.MULTI_EXCEPTION;
        ErrorResponse errorResponse =
                new ErrorResponse(
                        multiException.getStatus(), multiException.getCode(), errorMessage, url);
        log.info("MultiException", e);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request)
            throws IOException {
        final ContentCachingRequestWrapper cachingRequest = (ContentCachingRequestWrapper) request;
        final Long userId = SecurityUtils.getCurrentUserId();

        String url =
                UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
                        .build()
                        .toUriString();

        log.error("INTERNAL_SERVER_ERROR", e);
        GlobalErrorCode internalServerError = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse =
                new ErrorResponse(
                        internalServerError.getStatus(),
                        internalServerError.getCode(),
                        internalServerError.getReason(),
                        url);
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            slackInternalErrorSender.execute(cachingRequest, e, userId);
        }
        return ResponseEntity.status(HttpStatus.valueOf(internalServerError.getStatus()))
                .body(errorResponse);
    }
}
