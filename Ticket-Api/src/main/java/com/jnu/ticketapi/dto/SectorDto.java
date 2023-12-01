package com.jnu.ticketapi.dto;


import lombok.Builder;

@Builder
public record SectorDto(Long sectorId, String sectorName, String sectionColleges) {}
