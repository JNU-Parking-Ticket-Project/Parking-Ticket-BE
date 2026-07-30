package com.jnu.ticketapi.api.event.service;


import com.jnu.ticketdomain.domains.events.domain.Sector;
import java.util.List;
import java.util.Set;

public record EventStockRecoverySnapshot(List<Sector> sectors, Set<String> reservedEmails) {}
