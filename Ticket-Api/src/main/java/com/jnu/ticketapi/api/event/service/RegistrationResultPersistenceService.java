package com.jnu.ticketapi.api.event.service;

import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NoEventStockLeftException;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.events.exception.NotOpenEventStatusException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationResultPersistenceService {

    private final RegistrationAdaptor registrationAdaptor;
    private final UserAdaptor userAdaptor;
    private final EmailOutboxAdaptor emailOutboxAdaptor;
    private final SectorAdaptor sectorAdaptor;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StockReservationResult persistRedisReservation(
            Registration registration,
            Long userId,
            Long sectorId,
            Long eventId,
            StockReservationResult reservation,
            long savedAt) {
        if (reservation == null || !reservation.isReserved()) {
            throw new IllegalArgumentException("A reserved Redis decision is required");
        }

        User user = userAdaptor.findByIdForUpdate(userId);
        Sector sector = sectorAdaptor.findByIdForUpdate(sectorId);
        validateSector(sector, eventId);

        Optional<Registration> existing =
                registrationAdaptor.findSavedByEmailAndEventId(registration.getEmail(), eventId);
        if (existing.isPresent()) {
            return toReservationResult(existing.get(), sector.getRemainingAmount());
        }

        Optional<Registration> positionOwner =
                registrationAdaptor.findSavedBySectorIdAndPosition(
                        sectorId, reservation.getPosition());
        if (positionOwner.isPresent()) {
            throw new IllegalStateException(
                    "Redis position is already assigned. eventId="
                            + eventId
                            + ", sectorId="
                            + sectorId
                            + ", position="
                            + reservation.getPosition());
        }

        int checkpointRemaining =
                Math.min(
                        currentRemaining(sector),
                        reservation.getRemainingAmount() != null
                                ? reservation.getRemainingAmount()
                                : sector.getIssueAmount() - reservation.getPosition());
        sector.syncRemainingAmount(checkpointRemaining);

        return saveResult(
                registration,
                user,
                sector,
                reservation.getPosition(),
                reservation.getResultStatus(),
                reservation.getSequence(),
                savedAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StockReservationResult persistWithDatabaseFallback(
            Registration registration,
            Long userId,
            Long sectorId,
            Long eventId,
            long savedAt) {
        User user = userAdaptor.findByIdForUpdate(userId);
        Sector sector = sectorAdaptor.findByIdForUpdate(sectorId);
        validateSector(sector, eventId);

        Optional<Registration> existing =
                registrationAdaptor.findSavedByEmailAndEventId(registration.getEmail(), eventId);
        if (existing.isPresent()) {
            return toReservationResult(existing.get(), sector.getRemainingAmount());
        }

        try {
            sector.decreaseEventStock();
        } catch (NoEventStockLeftException exception) {
            throw exception;
        }

        int position = sector.getIssueAmount() - sector.getRemainingAmount();
        RegistrationDecision decision = decide(sector, position);
        return saveResult(
                registration,
                user,
                sector,
                position,
                decision.resultStatus,
                decision.sequence,
                savedAt);
    }

    private StockReservationResult saveResult(
            Registration registration,
            User user,
            Sector sector,
            int position,
            UserStatus resultStatus,
            int sequence,
            long savedAt) {
        reflectUserState(user, resultStatus, sequence);
        registration.finalSave(position, resultStatus, sequence);
        registration.setSector(sector);
        registration.setUser(user);
        registration.setSavedAt(savedAt);

        Registration savedRegistration = registrationAdaptor.saveAndFlush(registration);
        emailOutboxAdaptor.saveRegistrationResultIfAbsent(savedRegistration);
        return StockReservationResult.reserved(
                position, resultStatus, sequence, sector.getRemainingAmount());
    }

    private StockReservationResult toReservationResult(
            Registration registration, Integer remainingAmount) {
        return StockReservationResult.reserved(
                registration.getPosition(),
                registration.getResultStatus(),
                registration.getSequence(),
                remainingAmount);
    }

    private RegistrationDecision decide(Sector sector, int position) {
        if (position <= sector.getInitSectorCapacity()) {
            return new RegistrationDecision(UserStatus.SUCCESS, -2);
        }
        if (position <= sector.getIssueAmount()) {
            return new RegistrationDecision(
                    UserStatus.PREPARE, position - sector.getInitSectorCapacity());
        }
        return new RegistrationDecision(UserStatus.FAIL, -1);
    }

    private void reflectUserState(User user, UserStatus resultStatus, int sequence) {
        if (resultStatus == UserStatus.SUCCESS) {
            user.success();
            return;
        }
        if (resultStatus == UserStatus.PREPARE) {
            user.prepare(sequence);
            return;
        }
        user.fail();
    }

    private void validateSector(Sector sector, Long eventId) {
        if (sector.getEvent() == null
                || !Objects.equals(sector.getEvent().getId(), eventId)) {
            throw NotFoundSectorException.EXCEPTION;
        }
        if (sector.getEvent().getEventStatus() != EventStatus.OPEN) {
            throw NotOpenEventStatusException.EXCEPTION;
        }
    }

    private int currentRemaining(Sector sector) {
        return Optional.ofNullable(sector.getRemainingAmount()).orElse(sector.getIssueAmount());
    }

    private record RegistrationDecision(UserStatus resultStatus, int sequence) {}
}
