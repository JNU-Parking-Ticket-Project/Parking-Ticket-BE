package com.jnu.ticketdomain.domains.events.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.events.domain.Event;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.InvalidSizeSectorException;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.events.out.SectorLoadPort;
import com.jnu.ticketdomain.domains.events.out.SectorRecordPort;
import com.jnu.ticketdomain.domains.events.repository.SectorRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class SectorAdaptor implements SectorRecordPort, SectorLoadPort {
    private final SectorRepository sectorRepository;
    private final EventAdaptor eventAdaptor;

    public Sector findById(Long sectorId) {
        return sectorRepository
                .findById(sectorId)
                .orElseThrow(() -> NotFoundSectorException.EXCEPTION);
    }

    @Override
    public List<Sector> findAll() {
        return sectorRepository.findAll();
    }

    @Override
    public List<Sector> findByEventId(Long eventId) {
        return sectorRepository.findByEventId(eventId);
    }

    @Override
    public List<Sector> findRecentSector() {
        Event recentEvent = eventAdaptor.findRecentEvent();
        if (recentEvent == null) throw NotFoundSectorException.EXCEPTION;
        return sectorRepository.findByEventId(recentEvent.getId());
    }

    @Override
    public List<Sector> findAllByEventStatusAndPublishAndIsDeleted() {
        List<Sector> sectors = sectorRepository.findAllByEventStatus();
        return sectors.stream()
                .filter(sector -> sector.getEvent().getPublish() && !sector.getEvent().isDeleted())
                .toList();
    }

    @Override
    public Sector save(Sector coupon) {
        return sectorRepository.save(coupon);
    }

    @Override
    public List<Sector> saveAll(List<Sector> sectorList) {
        return sectorRepository.saveAll(sectorList);
    }

    @Override
    public void updateAll(List<Sector> prevSector, List<Sector> sectorList) {
        if (prevSector.size() != sectorList.size()) {
            throw InvalidSizeSectorException.EXCEPTION;
        }

        List<CompletableFuture<Void>> updateFutures =
                IntStream.range(0, prevSector.size())
                        .parallel()
                        .mapToObj(
                                index ->
                                        CompletableFuture.runAsync(
                                                () -> updateSector(prevSector, sectorList, index)))
                        .toList();

        CompletableFuture.allOf(updateFutures.toArray(new CompletableFuture[0])).join();
    }

    private void updateSector(List<Sector> prevSector, List<Sector> sectorList, int index) {
        prevSector.get(index).update(sectorList.get(index));
    }

    //    @Override
    //    public void updateAll(List<Sector> prevSector, List<Sector> sectorList) {
    //        List<CompletableFuture<Void>> updateFutures =
    //            List<CompletableFuture<Void>> updateFutures = IntStream.range(0,
    // prevSector.size())
    //                .parallel()
    //                .mapToObj(index -> CompletableFuture.runAsync(() ->
    // prevSector.get(index).update(sectorList.get(index))))
    //                .collect(Collectors.toList());
    //    //                prevSector.parallelStream()
    //    //                        .map(
    //    //                                prev ->
    //    //                                        CompletableFuture.runAsync(
    //    //                                                () -> {
    //    //                                                    Sector updatedSector =
    //    //                                                            findMatchingSector(prev,
    // sectorList);
    //    //                                                    if (updatedSector != null) {
    //    //                                                        prev.update(updatedSector);
    //    //                                                    }
    //    //                                                }))
    //    //                        .toList();
    //                            CompletableFuture.allOf(updateFutures.toArray(new
    // CompletableFuture[0]))
    //                                .join();
    //
    //                            ;
    //                    } private static Sector findMatchingSector(Sector prevSector, List<Sector>
    // newSectors) {
    //        return newSectors.stream()
    //                .filter(newSector -> newSector.getName().equals(prevSector.getName()))
    //                .findFirst()
    //                .orElse(null);
    //    }

    @Override
    public void delete(Long sectorId) {
        sectorRepository.delete(sectorId);
    }

    @Override
    public void deleteByEvent(Long eventId) {
        sectorRepository.deleteByEventId(eventId);
    }
}
