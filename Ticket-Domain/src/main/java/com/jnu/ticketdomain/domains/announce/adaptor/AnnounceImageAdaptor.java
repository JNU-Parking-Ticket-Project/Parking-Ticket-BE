package com.jnu.ticketdomain.domains.announce.adaptor;


import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import com.jnu.ticketdomain.domains.announce.out.AnnounceImageLordPort;
import com.jnu.ticketdomain.domains.announce.out.AnnounceImageRecordPort;
import com.jnu.ticketdomain.domains.announce.repository.AnnounceImageNativeRepository;
import com.jnu.ticketdomain.domains.announce.repository.AnnounceImageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adaptor
public class AnnounceImageAdaptor implements AnnounceImageLordPort, AnnounceImageRecordPort {

    private final AnnounceImageRepository announceImageRepository;
    private final AnnounceImageNativeRepository announceImageNativeRepository;

    @Override
    public List<AnnounceImage> saveAll(List<AnnounceImage> announceImages) {
        return announceImageRepository.saveAll(announceImages);
    }

    @Override
    public List<AnnounceImage> updateAll(List<AnnounceImage> announceImages) {
        if (announceImages.isEmpty()) {
            return announceImages;
        }
        announceImageNativeRepository.deleteNotFoundImage(announceImages);
        announceImageNativeRepository.saveAllDuplicateOn(announceImages);

        return announceImages;
    }

    @Override
    public List<AnnounceImage> findByAnnounce(Announce announce) {
        return announceImageRepository.findByAnnounce(announce);
    }
}
