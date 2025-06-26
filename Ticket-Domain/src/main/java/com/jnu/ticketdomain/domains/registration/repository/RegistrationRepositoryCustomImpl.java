package com.jnu.ticketdomain.domains.registration.repository;

import com.jnu.ticketdomain.domains.events.domain.QSector;
import com.jnu.ticketdomain.domains.registration.domain.QRegistration;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.QUser;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

import static com.jnu.ticketdomain.domains.registration.domain.QRegistration.registration;

@Repository
@RequiredArgsConstructor
public class RegistrationRepositoryCustomImpl implements RegistrationRepositoryCustom {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    @Override
    public Boolean existsByEmailAndIsSavedTrueAndEvent(String email, Long eventId) {
        return queryFactory
                        .selectOne()
                        .from(registration)
                        .where(registration.email.eq(email).and(isSavedAndEqEvent(registration, eventId)))
                        .fetchFirst()
                != null;
    }

    @Override
    public Boolean existsByStudentNumAndIsSavedTrue(String studentNum, Long eventId) {
        return queryFactory
                        .selectOne()
                        .from(registration)
                        .where(
                                registration.studentNum
                                        .eq(studentNum)
                                        .and(isSavedAndEqEvent(registration, eventId)))
                        .fetchFirst()
                != null;
    }

    @Override
    public List<Registration> findSortedRegistrationsByEventId(Long eventId) {

        QUser user = registration.user;
        QSector sector = registration.sector;

        return queryFactory
                .selectFrom(registration)
                .where(
                        registration.isDeleted.isFalse(),
                        registration.isSaved.isTrue(),
                        sector.event.id.eq(eventId),
                        user.status.in(UserStatus.SUCCESS, UserStatus.PREPARE))
                .orderBy(
                        sector.sectorNumber.asc(),
                        // SUCCESS 먼저 오도록 정렬
                        new CaseBuilder()
                                .when(user.status.eq(UserStatus.SUCCESS))
                                .then(0)
                                .otherwise(1)
                                .asc(),
                        // SUCCESS면 r.id, PREPARE면 u.sequence
                        new CaseBuilder()
                                .when(user.status.eq(UserStatus.SUCCESS))
                                .then(registration.id)
                                .otherwise(user.sequence.longValue())
                                .asc())
                .fetch();
    }

    private BooleanExpression isSavedAndEqEvent(QRegistration registration, Long eventId) {
        return registration.isSaved.isTrue().and(registration.sector.event.id.eq(eventId));
    }
}
