package com.jnu.ticketdomain.domains.registration.repository;

import static com.jnu.ticketdomain.domains.registration.domain.QRegistrationResultEmail.registrationResultEmail;

import com.jnu.ticketdomain.domains.registration.domain.RegistrationResultEmail;
import com.jnu.ticketdomain.domains.registration.domain.TransferStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RegistrationResultEmailRepositoryCustomImpl
        implements RegistrationResultEmailRepositoryCustom {
    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    @Override
    public List<RegistrationResultEmail> findOutboxEmailsByEventId(long eventId, int fetchSize) {
        return queryFactory
                .selectFrom(registrationResultEmail)
                .where(
                        registrationResultEmail
                                .eventId
                                .eq(eventId),
                        registrationResultEmail
                                .transferStatus
                                .in(TransferStatus.PENDING, TransferStatus.FAILED_1, TransferStatus.FAILED_2, TransferStatus.FAILED_3)
                )
                .orderBy(registrationResultEmail.transferStatus.asc(), registrationResultEmail.emailId.asc())
                .limit(fetchSize) // PENDING -> FAILED 순으로 정렬 (transferStatus는 EnumType.ORDINAL)
                .fetch();
    }
}
