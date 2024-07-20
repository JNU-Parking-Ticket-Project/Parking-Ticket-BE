package com.jnu.ticketcommon.message;

public class ValidationMessage {
    public static final String INVALID_ROLE_VALUE = "ROLE을 정확히 입력해 주세요.";

    public static final String MUST_NOT_BLANK = "입력해 주세요.";

    public static final String IS_NOT_VALID_PASSWORD =
            "비밀번호는 최소 8자 이상 최대 20자 이하이며, 최소 하나의 대문자, 소문자, 숫자, 특수 문자를 포함해야 합니다.";

    public static final String IS_NOT_VALID_EMAIL = "올바른 형식의 이메일을 입력해 주세요";

    public static final String MUST_NOT_NULL = "입력/선택해 주세요.";

    public static final String IS_NOT_VALID_PHONE = "올바른 형식의 전화번호를 입력해 주세요.";

    public static final String MUST_POSITIVE_NUMBER = "양수만 입력 가능 합니다.";

    public static final String TITLE_MUST_NOT_BLANK = "제목을 " + MUST_NOT_BLANK;

    public static final String DATE_MUST_NOT_NULL = "날짜를 " + MUST_NOT_NULL;

    public static final String MUST_NOT_OVER_100 = "100자 이하로 입력해 주세요.";

    private ValidationMessage() {}
}
