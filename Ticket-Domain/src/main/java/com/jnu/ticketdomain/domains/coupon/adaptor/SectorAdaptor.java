package com.jnu.ticketdomain.domains.coupon.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.coupon.domain.Sector;
import com.jnu.ticketdomain.domains.coupon.exception.NotFoundSectorException;
import com.jnu.ticketdomain.domains.coupon.out.SectorLoadPort;
import com.jnu.ticketdomain.domains.coupon.out.SectorRecordPort;
import com.jnu.ticketdomain.domains.coupon.repository.SectorRepository;
import java.util.List;
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
        couponRepository.saveAll(prevSector);
    }

    @Override
    public void delete(Sector sector) {
        couponRepository.delete(sector);
    }
}
