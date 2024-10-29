package com.jnu.ticketdomain.domains.announce.adaptor;

import static com.jnu.ticketdomain.domains.announce.repository.AnnounceImageRepository.INSERT_DUPLICATE_ON;

import com.jnu.ticketcommon.annotation.Adaptor;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import com.jnu.ticketdomain.domains.announce.out.AnnounceImageLordPort;
import com.jnu.ticketdomain.domains.announce.out.AnnounceImageRecordPort;
import com.jnu.ticketdomain.domains.announce.repository.AnnounceImageRepository;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Adaptor
public class AnnounceImageAdaptor implements AnnounceImageLordPort, AnnounceImageRecordPort {

    private final AnnounceImageRepository announceImageRepository;
    private final EntityManager entityManager;

    @Override
    public List<AnnounceImage> saveAll(List<AnnounceImage> announceImages) {
        return announceImageRepository.saveAll(announceImages);
    }

    @Override
    public List<AnnounceImage> updateAll(List<AnnounceImage> announceImages) {
        if (announceImages.isEmpty()) {
            return announceImages;
        }
        entityManager.getTransaction().begin();
        entityManager
                .createQuery(DELETE_NOT_FOUND_IMAGE)
                .setParameter(
                        "url",
                        announceImages.stream()
                                .map(AnnounceImage::getImageUrl)
                                .collect(Collectors.toList()))
                .executeUpdate();

        for (AnnounceImage announceImage : announceImages) {
            entityManager
                    .createNativeQuery(INSERT_DUPLICATE_ON)
                    .setParameter(1, announceImage.getImageUrl())
                    .setParameter(2, announceImage.getAnnounce().getAnnounceId())
                    .executeUpdate();
        }

        return announceImages;
    }

    @Override
    public List<AnnounceImage> findByAnnounce(Announce announce) {
        return announceImageRepository.findByAnnounce(announce);
    }
}
