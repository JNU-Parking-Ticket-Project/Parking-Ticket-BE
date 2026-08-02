package com.jnu.ticketapi.api.event.service;

public class RedisAdmissionInvariantException extends IllegalStateException {
    public RedisAdmissionInvariantException(Long sectorId, String detail) {
        super("Redis 신청 재고 불변식이 깨졌습니다. sectorId=" + sectorId + ", detail=" + detail);
    }
}
