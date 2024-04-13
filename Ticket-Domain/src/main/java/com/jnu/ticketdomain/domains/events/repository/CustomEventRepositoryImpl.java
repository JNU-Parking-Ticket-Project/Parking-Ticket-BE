package com.jnu.ticketdomain.domains.events.repository;

import static com.jnu.ticketdomain.domains.events.domain.QEvent.event;

import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CustomEventRepositoryImpl implements CustomEventRepository {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    @Override
    public Boolean existsByPublishTrueAndStatus() {
        return queryFactory.selectOne().from(event).where(isPublishTrueAndStatus()).fetchFirst()
                != null;
    }

    private BooleanExpression isPublishTrueAndStatus() {
        return event.publish
                .isTrue()
                .and(event.eventStatus.in(EventStatus.OPEN, EventStatus.READY));
    }
}
