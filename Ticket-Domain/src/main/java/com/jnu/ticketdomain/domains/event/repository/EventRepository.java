package com.jnu.ticketdomain.domains.event.repository;


import com.jnu.ticketdomain.domains.event.domain.Event;
import com.jnu.ticketdomain.domains.event.domain.EventStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByCouponStatus(EventStatus eventStatus);
}
