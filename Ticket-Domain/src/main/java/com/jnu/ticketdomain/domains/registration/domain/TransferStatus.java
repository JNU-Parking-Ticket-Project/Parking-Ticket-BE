package com.jnu.ticketdomain.domains.registration.domain;

public enum TransferStatus {
    PENDING, // 전송 대기
    FAILED_1, // 1차 전송 실패
    FAILED_2,
    FAILED_3,
    EXCLUDED, // 전송 실패 횟수가 임계점을 넣었거나, 기타 이유로 전송되지 못하고 전송 대상에서 제외
    SUCCESS // 전송 성공
}
