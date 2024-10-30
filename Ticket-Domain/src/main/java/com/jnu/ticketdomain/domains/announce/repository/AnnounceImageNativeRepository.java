package com.jnu.ticketdomain.domains.announce.repository;


import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AnnounceImageNativeRepository {

    private final EntityManager entityManager;

    private static final String DELETE_NOT_FOUND_IMAGE =
            "DELETE FROM AnnounceImage i WHERE i.imageUrl NOT IN :imageUrl";
    private static final String INSERT_DUPLICATE_ON =
            "INSERT INTO ANNOUNCE_IMAGE_TB(URL, ANNOUNCE_ID) VALUES (?,?)"
                    + "ON DUPLICATE KEY UPDATE URL = VALUES(URL)";

    @Transactional
    public void deleteNotFoundImage(List<AnnounceImage> announceImages) {
        entityManager
                .createQuery(DELETE_NOT_FOUND_IMAGE)
                .setParameter(
                        "imageUrl",
                        announceImages.stream()
                                .map(AnnounceImage::getImageUrl)
                                .collect(Collectors.toList()))
                .executeUpdate();
    }

    @Transactional
    public void saveAllDuplicateOn(List<AnnounceImage> announceImages) {
        for (AnnounceImage announceImage : announceImages) {
            entityManager
                    .createNativeQuery(INSERT_DUPLICATE_ON)
                    .setParameter(1, announceImage.getImageUrl())
                    .setParameter(2, announceImage.getAnnounce().getAnnounceId())
                    .executeUpdate();
        }
    }
}
