package com.jnu.ticketapi.api.sector.model.request;


import lombok.Builder;

@Builder
public record SectorReadRequest(Long sectorId, String sectorNum, String sectorName) {}
