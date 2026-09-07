package com.jnu.ticketdomain.domains.registration.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionJournal;
import com.jnu.ticketdomain.domains.registration.domain.RegistrationAdmissionState;
import com.jnu.ticketdomain.domains.registration.repository.RegistrationAdmissionJournalRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

@Adaptor
@RequiredArgsConstructor
public class RegistrationAdmissionJournalAdaptor {
    private final RegistrationAdmissionJournalRepository repository;

    public RegistrationAdmissionJournal saveAndFlush(
            RegistrationAdmissionJournal admissionJournal) {
        return repository.saveAndFlush(admissionJournal);
    }

    public Optional<RegistrationAdmissionJournal> findByEventIdAndEmail(
            Long eventId, String email) {
        return repository.findByEventIdAndEmail(eventId, email);
    }

    public boolean existsByEventIdAndEmail(Long eventId, String email) {
        return repository.existsByEventIdAndEmail(eventId, email);
    }

    public RegistrationAdmissionJournal findByIdForUpdate(Long journalId) {
        return repository
                .findByIdForUpdate(journalId)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "신청 결정 저널을 찾을 수 없습니다. journalId=" + journalId));
    }

    public int findMaxPositionBySectorId(Long sectorId) {
        return Optional.ofNullable(
                        repository.findMaxPositionBySectorIdAndStates(
                                sectorId,
                                List.of(
                                        RegistrationAdmissionState.DECIDED,
                                        RegistrationAdmissionState.MATERIALIZED)))
                .orElse(0);
    }

    public List<Integer> findDecidedPositionsBySectorId(Long sectorId) {
        return repository.findPositionsBySectorIdAndStates(
                sectorId,
                List.of(
                        RegistrationAdmissionState.DECIDED,
                        RegistrationAdmissionState.MATERIALIZED));
    }

    public List<RegistrationAdmissionJournal> findDecidedByEventId(
            Long eventId, long decidedAtOrBefore) {
        return repository.findAllByEventIdAndStateAndDecidedAtLessThanEqualOrderByIdAsc(
                eventId, RegistrationAdmissionState.DECIDED, decidedAtOrBefore);
    }

    public List<RegistrationAdmissionJournal> findReceivedThrough(
            Long eventId, long throughJournalId) {
        return repository.findAllByEventIdAndStateAndIdLessThanEqualOrderByIdAsc(
                eventId, RegistrationAdmissionState.RECEIVED, throughJournalId);
    }

    public List<RegistrationAdmissionJournal> findDecidedBatch(
            long decidedAtOrBefore, long afterJournalId, long throughJournalId) {
        return repository.findBatchForMaterialization(
                RegistrationAdmissionState.DECIDED,
                decidedAtOrBefore,
                afterJournalId,
                throughJournalId,
                PageRequest.of(0, 100));
    }

    public long findMaxDecidedId(long decidedAtOrBefore) {
        return Optional.ofNullable(
                        repository.findMaxIdForMaterialization(
                                RegistrationAdmissionState.DECIDED, decidedAtOrBefore))
                .orElse(0L);
    }
}
