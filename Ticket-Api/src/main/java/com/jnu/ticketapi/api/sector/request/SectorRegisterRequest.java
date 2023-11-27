package com.jnu.ticketapi.api.sector.request;


import lombok.Builder;

public record SectorRegisterRequest(
        String sectorNumber, String name, Integer sectorCapacity, Integer reserve) {
    @Builder
    public SectorRegisterRequest {}
}
