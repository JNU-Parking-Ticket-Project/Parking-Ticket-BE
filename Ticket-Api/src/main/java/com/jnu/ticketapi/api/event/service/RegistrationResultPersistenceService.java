package com.jnu.ticketapi.api.event.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnu.ticketdomain.domains.email.adaptor.EmailOutboxAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.EventAdaptor;
import com.jnu.ticketdomain.domains.events.adaptor.SectorAdaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.EventAdmissionMode;
import com.jnu.ticketdomain.domains.events.domain.EventStatus;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.events.exception.NotOpenEventStatusException;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdaptor;
import com.jnu.ticketdomain.domains.registration.adaptor.RegistrationAdmissionJournalAdaptor;
import com.jnu.ticketdomain.domains.registration.domain.Registration;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionJournal;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationDecisionSource;
import com.jnu.ticketdomain.domains.user.adaptor.UserAdaptor;
import com.jnu.ticketdomain.domains.user.domain.User;
import com.jnu.ticketdomain.domains.user.domain.UserStatus;
import com.jnu.ticketinfrastructure.model.StockReservationResult;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final RegistrationAdmissionJournalAdaptor admissionJournalAdaptor;
    private final EventAdaptor eventAdaptor;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StockReservationResult persistRedisReservation(
            Registration registration,
            Long userId,
            Long sectorId,
            Long eventId,
            StockReservationResult reservation,
            long savedAt) {
        validateReservedDecision(reservation);

        Event event = eventAdaptor.findByIdForAdmissionRead(eventId);
        if (!event.isRedisAdmission(event.getAdmissionEpoch())) {
            throw new AdmissionEpochChangedException(eventId, null);
        }

        User user = userAdaptor.findByIdForUpdate(userId);
        Sector sector = sectorAdaptor.findByIdForUpdate(sectorId);
        validateSectorOwnership(sector, eventId);
        validateReservationAgainstSector(reservation, sector, false);

        Optional<Registration> existing =
                registrationAdaptor.findSavedByEmailAndEventId(registration.getEmail(), eventId);
        if (existing.isPresent()) {
            return toReservationResult(existing.get(), sector.getRemainingAmount());
        }
        validatePositionOwner(sectorId, reservation.getPosition());
        applyCheckpoint(sector, reservation);

        Registration saved =
                saveRegistration(
                        registration,
                        user,
                        sector,
                        reservation.getPosition(),
                        reservation.getResultStatus(),
                        reservation.getSequence(),
                        savedAt);
        return toReservationResult(saved, sector.getRemainingAmount());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StockReservationResult confirmRedisDecision(
            Long journalId, StockReservationResult reservation, long decidedAt) {
        validateReservedDecision(reservation);
        RegistrationAdmissionJournal journal = admissionJournalAdaptor.findByIdForUpdate(journalId);
        if (journal.isRejected()) {
            return toRejectedResult(journal);
        }
        if (journal.isDecided()) {
            if (journal.getDecisionSource() == RegistrationDecisionSource.REDIS) {
                validateJournalDecision(journal, RegistrationDecisionSource.REDIS, reservation);
            }
            return toReservationResult(journal);
        }
        Event event = eventAdaptor.findByIdForAdmissionRead(journal.getEventId());
        if (!event.isRedisAdmission(journal.getAdmissionEpoch())) {
            throw new AdmissionEpochChangedException(event.getId(), journalId);
        }
        Sector sector = sectorAdaptor.findById(journal.getSectorId());
        validateSectorOwnership(sector, journal.getEventId());
        validateReservationAgainstSector(reservation, sector, true);
        journal.confirm(
                RegistrationDecisionSource.REDIS,
                reservation.getPosition(),
                reservation.getResultStatus(),
                reservation.getSequence(),
                reservation.getRemainingAmount(),
                decidedAt);
        admissionJournalAdaptor.saveAndFlush(journal);
        return reservation;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StockReservationResult persistJournalWithDatabaseFallback(Long journalId, long savedAt) {
        RegistrationAdmissionJournal journal = admissionJournalAdaptor.findByIdForUpdate(journalId);
        if (journal.isRejected()) {
            return toRejectedResult(journal);
        }
        if (journal.isDecided()) {
            return materializeConfirmed(journal, restoreRegistration(journal), savedAt);
        }

        Event event = eventAdaptor.findByIdForAdmissionRead(journal.getEventId());
        if (event.getAdmissionMode() != EventAdmissionMode.DB_FALLBACK) {
            throw new IllegalStateException("DB fallback 모드가 아닌 이벤트입니다. eventId=" + event.getId());
        }

        User user = userAdaptor.findByIdForUpdate(journal.getUserId());
        Sector sector = sectorAdaptor.findByIdForUpdate(journal.getSectorId());
        validateFallbackSector(sector, journal.getEventId());
        Registration registration = restoreRegistration(journal);
        Optional<Registration> existing =
                registrationAdaptor.findSavedByEmailAndEventId(
                        journal.getEmail(), journal.getEventId());
        if (existing.isPresent()) {
            journal.reject("DUPLICATE", sector.getRemainingAmount(), savedAt);
            return StockReservationResult.duplicate(sector.getRemainingAmount());
        }

        Set<Integer> occupiedPositions = occupiedPositions(journal.getSectorId());
        Integer position = nextAvailablePosition(sector.getIssueAmount(), occupiedPositions);
        if (position == null) {
            journal.reject("NO_STOCK", 0, savedAt);
            return StockReservationResult.noStock(0);
        }
        sector.syncRemainingAmount(
                Math.max(0, sector.getIssueAmount() - occupiedPositions.size() - 1));
        RegistrationDecision decision = decide(sector, position);
        journal.confirm(
                RegistrationDecisionSource.DATABASE,
                position,
                decision.resultStatus,
                decision.sequence,
                sector.getRemainingAmount(),
                savedAt);
        admissionJournalAdaptor.saveAndFlush(journal);
        Registration saved =
                saveRegistration(
                        registration,
                        user,
                        sector,
                        position,
                        decision.resultStatus,
                        decision.sequence,
                        savedAt);
        journal.markMaterialized(saved.getId(), System.currentTimeMillis());
        return toReservationResult(saved, sector.getRemainingAmount());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StreamDecisionAction recordStreamDecision(
            Long journalId,
            Long streamAdmissionEpoch,
            StockReservationResult reservation,
            long savedAt) {
        validateReservedDecision(reservation);
        RegistrationAdmissionJournal journal = admissionJournalAdaptor.findByIdForUpdate(journalId);
        if (journal.isMaterialized() || journal.isRejected()) {
            return StreamDecisionAction.ACK_ONLY;
        }
        if (journal.isReceived()) {
            Event event = eventAdaptor.findByIdForAdmissionRead(journal.getEventId());
            if (!Objects.equals(streamAdmissionEpoch, journal.getAdmissionEpoch())
                    || !event.isRedisAdmission(journal.getAdmissionEpoch())) {
                return event.getAdmissionMode() == EventAdmissionMode.DB_FALLBACK
                        ? StreamDecisionAction.DATABASE_FALLBACK
                        : StreamDecisionAction.ACK_ONLY;
            }
            Sector sector = sectorAdaptor.findById(journal.getSectorId());
            validateSectorOwnership(sector, journal.getEventId());
            validateReservationAgainstSector(reservation, sector, true);
            journal.confirm(
                    RegistrationDecisionSource.REDIS,
                    reservation.getPosition(),
                    reservation.getResultStatus(),
                    reservation.getSequence(),
                    reservation.getRemainingAmount(),
                    savedAt);
            admissionJournalAdaptor.saveAndFlush(journal);
            return StreamDecisionAction.DEFER_MATERIALIZATION;
        }
        if (journal.getDecisionSource() != RegistrationDecisionSource.REDIS) {
            return StreamDecisionAction.ACK_ONLY;
        }
        validateJournalDecision(journal, RegistrationDecisionSource.REDIS, reservation);
        return StreamDecisionAction.MATERIALIZE;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StockReservationResult materializeConfirmedJournal(Long journalId) {
        RegistrationAdmissionJournal journal = admissionJournalAdaptor.findByIdForUpdate(journalId);
        if (journal.isMaterialized()) {
            return toReservationResult(journal);
        }
        if (!journal.isDecided()) {
            throw new IllegalStateException("확정되지 않은 신청 저널은 본 저장할 수 없습니다. journalId=" + journalId);
        }
        return materializeConfirmed(journal, restoreRegistration(journal), journal.getDecidedAt());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StockReservationResult persistWithDatabaseFallback(
            Registration registration, Long userId, Long sectorId, Long eventId, long savedAt) {
        User user = userAdaptor.findByIdForUpdate(userId);
        Sector sector = sectorAdaptor.findByIdForUpdate(sectorId);
        validateOpenSector(sector, eventId);

        Optional<Registration> existing =
                registrationAdaptor.findSavedByEmailAndEventId(registration.getEmail(), eventId);
        if (existing.isPresent()) {
            return StockReservationResult.duplicate(sector.getRemainingAmount());
        }

        Set<Integer> occupiedPositions = occupiedPositions(sectorId);
        Integer position = nextAvailablePosition(sector.getIssueAmount(), occupiedPositions);
        if (position == null) {
            return StockReservationResult.noStock(0);
        }
        sector.syncRemainingAmount(
                Math.max(0, sector.getIssueAmount() - occupiedPositions.size() - 1));
        RegistrationDecision decision = decide(sector, position);
        Registration saved =
                saveRegistration(
                        registration,
                        user,
                        sector,
                        position,
                        decision.resultStatus,
                        decision.sequence,
                        savedAt);
        return toReservationResult(saved, sector.getRemainingAmount());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventStockRecoverySnapshot prepareRecoverySnapshot(Long eventId) {
        List<Registration> savedRegistrations =
                registrationAdaptor.findSavedForAdmissionRecovery(eventId);
        Map<Long, List<Registration>> registrationsBySector =
                savedRegistrations.stream()
                        .collect(
                                Collectors.groupingBy(
                                        registration -> registration.getSector().getId()));
        List<Sector> sectors =
                sectorAdaptor.findByEventId(eventId).stream()
                        .sorted(Comparator.comparing(Sector::getId))
                        .map(sector -> sectorAdaptor.findByIdForUpdate(sector.getId()))
                        .toList();
        if (sectors.isEmpty()) {
            throw NotFoundSectorException.EXCEPTION;
        }

        for (Sector sector : sectors) {
            validateOpenSector(sector, eventId);
            List<Registration> sectorRegistrations =
                    registrationsBySector.getOrDefault(sector.getId(), List.of());
            int maxPosition =
                    sectorRegistrations.stream()
                            .map(Registration::getPosition)
                            .filter(Objects::nonNull)
                            .max(Integer::compareTo)
                            .orElse(0);
            int assignedFromCheckpoint = sector.getIssueAmount() - currentRemaining(sector);
            int assignedPosition =
                    Math.min(
                            sector.getIssueAmount(),
                            Math.max(
                                    assignedFromCheckpoint,
                                    Math.max(maxPosition, sectorRegistrations.size())));
            sector.syncRemainingAmount(sector.getIssueAmount() - assignedPosition);
        }

        Set<String> reservedEmails =
                savedRegistrations.stream()
                        .map(Registration::getEmail)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableSet());
        return new EventStockRecoverySnapshot(List.copyOf(sectors), reservedEmails);
    }

    private StockReservationResult materializeConfirmed(
            RegistrationAdmissionJournal journal, Registration registration, long savedAt) {
        if (journal.isMaterialized()) {
            return toReservationResult(journal);
        }
        User user = userAdaptor.findByIdForUpdate(journal.getUserId());
        Sector sector = sectorAdaptor.findByIdForUpdate(journal.getSectorId());
        validateSectorOwnership(sector, journal.getEventId());

        Optional<Registration> existing =
                registrationAdaptor.findSavedByEmailAndEventId(
                        journal.getEmail(), journal.getEventId());
        if (existing.isPresent()) {
            Registration saved = existing.get();
            if (!Objects.equals(saved.getPosition(), journal.getPosition())
                    || saved.getResultStatus() != journal.getResultStatus()
                    || !Objects.equals(saved.getSequence(), journal.getSequence())) {
                throw new IllegalStateException("저널과 기존 신청 결과가 다릅니다. journalId=" + journal.getId());
            }
            journal.markMaterialized(saved.getId(), System.currentTimeMillis());
            return toReservationResult(saved, sector.getRemainingAmount());
        }

        validatePositionOwner(journal.getSectorId(), journal.getPosition());
        applyCheckpoint(sector, toReservationResult(journal));
        Registration saved =
                saveRegistration(
                        registration,
                        user,
                        sector,
                        journal.getPosition(),
                        journal.getResultStatus(),
                        journal.getSequence(),
                        savedAt);
        journal.markMaterialized(saved.getId(), System.currentTimeMillis());
        return toReservationResult(saved, sector.getRemainingAmount());
    }

    private Registration saveRegistration(
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
        return savedRegistration;
    }

    private Registration restoreRegistration(RegistrationAdmissionJournal journal) {
        if (!Integer.valueOf(1).equals(journal.getPayloadVersion())) {
            throw new IllegalStateException(
                    "지원하지 않는 신청 payload 버전입니다. journalId=" + journal.getId());
        }
        try {
            return objectMapper.readValue(journal.getRegistrationPayload(), Registration.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "신청 결정 저널 payload를 복원할 수 없습니다. journalId=" + journal.getId(), exception);
        }
    }

    private void validatePositionOwner(Long sectorId, Integer position) {
        Optional<Registration> positionOwner =
                registrationAdaptor.findSavedBySectorIdAndPosition(sectorId, position);
        if (positionOwner.isPresent()) {
            throw new IllegalStateException(
                    "Admission position is already assigned. sectorId="
                            + sectorId
                            + ", position="
                            + position);
        }
    }

    private void applyCheckpoint(Sector sector, StockReservationResult reservation) {
        int checkpointRemaining =
                Math.min(
                        currentRemaining(sector),
                        reservation.getRemainingAmount() != null
                                ? reservation.getRemainingAmount()
                                : sector.getIssueAmount() - reservation.getPosition());
        sector.syncRemainingAmount(checkpointRemaining);
    }

    private void validateJournalDecision(
            RegistrationAdmissionJournal journal,
            RegistrationDecisionSource source,
            StockReservationResult reservation) {
        if (journal.getDecisionSource() != source
                || !journal.matchesDecision(
                        reservation.getPosition(),
                        reservation.getResultStatus(),
                        reservation.getSequence())) {
            throw new IllegalStateException("저널과 전달된 신청 결과가 다릅니다. journalId=" + journal.getId());
        }
    }

    private void validateReservedDecision(StockReservationResult reservation) {
        if (reservation == null
                || !reservation.isReserved()
                || reservation.getPosition() == null
                || reservation.getResultStatus() == null
                || reservation.getSequence() == null) {
            throw new IllegalArgumentException("A reserved admission decision is required");
        }
    }

    private void validateReservationAgainstSector(
            StockReservationResult reservation, Sector sector, boolean requireRemainingAmount) {
        int position = reservation.getPosition();
        int issueAmount = sector.getIssueAmount();
        RegistrationDecision expected = decide(sector, position);
        if (position < 1
                || position > issueAmount
                || (requireRemainingAmount && reservation.getRemainingAmount() == null)
                || (reservation.getRemainingAmount() != null
                        && reservation.getRemainingAmount() + position != issueAmount)
                || reservation.getResultStatus() != expected.resultStatus
                || reservation.getSequence() == null
                || reservation.getSequence() != expected.sequence) {
            throw new RedisAdmissionInvariantException(
                    sector.getId(),
                    "position="
                            + position
                            + ", remaining="
                            + reservation.getRemainingAmount()
                            + ", status="
                            + reservation.getResultStatus()
                            + ", sequence="
                            + reservation.getSequence());
        }
    }

    private StockReservationResult toReservationResult(
            Registration registration, Integer remainingAmount) {
        return StockReservationResult.reserved(
                registration.getPosition(),
                registration.getResultStatus(),
                registration.getSequence(),
                remainingAmount);
    }

    private StockReservationResult toReservationResult(RegistrationAdmissionJournal journal) {
        return StockReservationResult.reserved(
                journal.getPosition(),
                journal.getResultStatus(),
                journal.getSequence(),
                journal.getRemainingAmount());
    }

    private StockReservationResult toRejectedResult(RegistrationAdmissionJournal journal) {
        return switch (journal.getDecisionReason()) {
            case "DUPLICATE" -> StockReservationResult.duplicate(journal.getRemainingAmount());
            case "NO_STOCK" -> StockReservationResult.noStock(journal.getRemainingAmount());
            case "CLOSED" -> StockReservationResult.closed(journal.getRemainingAmount());
            default -> StockReservationResult.unavailable(journal.getRemainingAmount());
        };
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

    private void validateOpenSector(Sector sector, Long eventId) {
        validateSectorOwnership(sector, eventId);
        if (sector.getEvent().getEventStatus() != EventStatus.OPEN) {
            throw NotOpenEventStatusException.EXCEPTION;
        }
    }

    private void validateFallbackSector(Sector sector, Long eventId) {
        validateSectorOwnership(sector, eventId);
        EventStatus status = sector.getEvent().getEventStatus();
        if (status != EventStatus.OPEN && status != EventStatus.CLOSED) {
            throw NotOpenEventStatusException.EXCEPTION;
        }
    }

    private void validateSectorOwnership(Sector sector, Long eventId) {
        if (sector.getEvent() == null || !Objects.equals(sector.getEvent().getId(), eventId)) {
            throw NotFoundSectorException.EXCEPTION;
        }
    }

    private int currentRemaining(Sector sector) {
        return Optional.ofNullable(sector.getRemainingAmount()).orElse(sector.getIssueAmount());
    }

    private Set<Integer> occupiedPositions(Long sectorId) {
        Set<Integer> positions =
                new HashSet<>(admissionJournalAdaptor.findDecidedPositionsBySectorId(sectorId));
        positions.addAll(registrationAdaptor.findSavedPositionsBySectorId(sectorId));
        positions.removeIf(position -> position == null || position < 1);
        return positions;
    }

    private Integer nextAvailablePosition(int issueAmount, Set<Integer> occupiedPositions) {
        for (int position = 1; position <= issueAmount; position++) {
            if (!occupiedPositions.contains(position)) {
                return position;
            }
        }
        return null;
    }

    public enum StreamDecisionAction {
        DEFER_MATERIALIZATION,
        MATERIALIZE,
        DATABASE_FALLBACK,
        ACK_ONLY
    }

    private record RegistrationDecision(UserStatus resultStatus, int sequence) {}
}
