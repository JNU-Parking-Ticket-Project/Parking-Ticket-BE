package com.jnu.ticketdomain.domains.registration.repository;

import static com.jnu.ticketdomain.domains.registration.domain.QRegistration.registration;

import com.jnu.ticketdomain.domains.registration.domain.QRegistration;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RegistrationRepositoryCustomImpl implements RegistrationRepositoryCustom {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    QRegistration r = QRegistration.registration;

    @Override
    public Boolean existsByEmailAndIsSavedTrueAndEvent(String email, Long eventId) {
        return queryFactory
                        .selectOne()
                        .from(r)
                        .where(r.email.eq(email).and(isSavedAndEqEvent(registration, eventId)))
                        .fetchFirst()
                != null;
    }

    @Override
    public Boolean existsByStudentNumAndIsSavedTrue(String studentNum, Long eventId) {
        return queryFactory
                        .selectOne()
                        .from(r)
                        .where(
                                r.studentNum
                                        .eq(studentNum)
                                        .and(isSavedAndEqEvent(registration, eventId)))
                        .fetchFirst()
                != null;
    }

    @Override
    public List<Registration> findSortedRegistrationsByEventId(Long eventId) {

        var u = r.user;
        var s = r.sector;

        return queryFactory
                .selectFrom(r)
                .where(
                        r.isDeleted.isFalse(),
                        r.isSaved.isTrue(),
                        s.event.id.eq(eventId),
                        u.status.in(UserStatus.SUCCESS, UserStatus.PREPARE))
                .orderBy(
                        s.sectorNumber.asc(),
                        // SUCCESS 먼저 오도록 정렬
                        new CaseBuilder()
                                .when(u.status.eq(UserStatus.SUCCESS))
                                .then(0)
                                .otherwise(1)
                                .asc(),
                        // SUCCESS면 r.id, PREPARE면 u.sequence
                        new CaseBuilder()
                                .when(u.status.eq(UserStatus.SUCCESS))
                                .then(r.id)
                                .otherwise(u.sequence.longValue())
                                .asc())
                .fetch();
    }

    private BooleanExpression isSavedAndEqEvent(QRegistration registration, Long eventId) {
        return registration.isSaved.isTrue().and(registration.sector.event.id.eq(eventId));
    }
}
