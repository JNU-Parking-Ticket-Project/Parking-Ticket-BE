package com.jnu.ticketdomain.domains.events.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.events.domain.Sector;
import com.jnu.ticketdomain.domains.events.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.events.out.SectorLoadPort;
import com.jnu.ticketdomain.domains.events.out.SectorRecordPort;
import com.jnu.ticketdomain.domains.events.repository.SectorRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class SectorAdaptor implements SectorRecordPort, SectorLoadPort {
    private final SectorRepository couponRepository;

    public Sector findById(Long couponId) {
        return couponRepository
                .findById(couponId)
                .orElseThrow(() -> NotFoundSectorException.EXCEPTION);
    }

    @Override
    public List<Sector> findAll() {
        return couponRepository.findAll();
    }

    @Override
    public Sector save(Sector coupon) {
        return couponRepository.save(coupon);
    }

    @Override
    public List<Sector> saveAll(List<Sector> sectorList) {
        return couponRepository.saveAll(sectorList);
    }

    @Override
    public void updateAll(List<Sector> prevSector, List<Sector> sectorList) {
        List<CompletableFuture<Void>> updateFutures =
                prevSector.parallelStream()
                        .map(
                                prev ->
                                        CompletableFuture.runAsync(
                                                () -> {
                                                    Sector updatedSector =
                                                            findMatchingSector(prev, sectorList);
                                                    if (updatedSector != null) {
                                                        prev.update(updatedSector);
                                                    }
                                                }))
                        .toList();
        CompletableFuture.allOf(updateFutures.toArray(new CompletableFuture[0])).join();
    }

    private static Sector findMatchingSector(Sector prevSector, List<Sector> newSectors) {
        return newSectors.stream()
                .filter(newSector -> newSector.getName().equals(prevSector.getName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void delete(Sector sector) {
        couponRepository.delete(sector);
    }
}
