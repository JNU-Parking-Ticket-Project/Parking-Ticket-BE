package com.jnu.ticketcommon.message;

public class ValidationMessage {
    public static final String INVALID_ROLE_VALUE = "ROLE을 정확히 입력 해주세요.";

    public static final String MUST_NOT_BLANK = "입력 해주세요.";

    public static final String IS_NOT_VALID_PASSWORD =
            "비밀번호는 최소 8자 이상 최대 20자 이하이며, 최소 하나의 대문자, 소문자, 숫자, 특수 문자를 포함해야 합니다.";

    public static final String IS_NOT_VALID_EMAIL = "이메일 형식이 올바르지 않습니다.";

    public static final String MUST_NOT_NULL = "입력/선택 해주세요.";

    public static final String IS_NOT_VALID_PHONE = "올바른 형식의 전화번호를 입력하세요.";

    public static final String MUST_POSITIVE_NUMBER = "양수만 입력 가능합니다.";

    private ValidationMessage() {}
}
