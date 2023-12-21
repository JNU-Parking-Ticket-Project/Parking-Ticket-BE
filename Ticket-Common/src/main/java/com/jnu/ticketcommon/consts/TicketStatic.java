package com.jnu.ticketcommon.consts;

public class TicketStatic {
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String TOKEN_ROLE = "role";
    public static final String TOKEN_TYPE = "type";
    public static final String TOKEN_ISSUER = "Ticket";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String SERVER = "Server";
    public static final String EMAIL_KEY = "Email";
    public static final String CONSTRAINT_VIOLATION_SEPARATOR = "\\.";
    public static final Integer DEVELOPER_COLUMNS_ID = 1;
    public static final Integer DESIGNER_COLUMNS_ID = 2;
    public static final Integer PLANNER_COLUMNS_ID = 3;
    public static final Integer COUNTS_PER_PAGE = 8;
    public static final Integer ANSWER_COUNTS_PER_PERSON = 33;
    public static final int MILLI_TO_SECOND = 1000;
    public static final int OK_REQUEST = 200;
    public static final int BAD_REQUEST = 400;
    public static final String BAD_REQUEST_CODE = "400";
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;

    public static final int NOT_FOUND = 404;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int INTERNAL_SERVER = 500;
    public static final String SWAGGER_DOCS_VERSION = "v0.0.1";

    public static final String REDIS_EVENT_CHANNEL = "쿠폰 발급 채널";
    public static final String REDIS_EVENT_ISSUE_STORE = "쿠폰 발급 저장소";
    public static final String[] SwaggerPatterns = {
        "/swagger-resources/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/v3/api-docs",
        "/api-docs/**",
        "/api-docs"
    };
    public static final String[] RolePattern = {
        "ROLE_ADMIN", "ROLE_MANAGER", "ROLE_GUEST", "ROLE_SWAGGER"
    };
}
