package com.jnu.ticketapi.api.sector.model.request;


import java.beans.ConstructorProperties;
import javax.validation.constraints.Min;
import lombok.Builder;

public record SectorRegisterRequest(
        String sectorNumber,
        String name,
        @Min(value = 0, message = "재고는 0 이상이어야 합니다.") Integer sectorCapacity,
        @Min(value = 0, message = "재고는 0 이상이어야 합니다.") Integer reserve) {
    @Builder
    @ConstructorProperties({"sectorNumber", "name", "sectorCapacity", "reserve"})
    public SectorRegisterRequest {}
}
