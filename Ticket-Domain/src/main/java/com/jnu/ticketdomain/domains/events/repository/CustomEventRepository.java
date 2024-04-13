package com.jnu.ticketdomain.domains.events.repository;


import org.springframework.stereotype.Repository;

@Repository
public interface CustomEventRepository {
    Boolean existsByPublishTrueAndStatus();
}
