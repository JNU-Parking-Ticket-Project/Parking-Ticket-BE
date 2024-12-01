package com.jnu.ticketdomain.domains.announce.repository;


import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import java.time.LocalDateTime;
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
    private static final String INSERT_IGNORE =
            "INSERT IGNORE INTO announce_image_tb (URL, ANNOUNCE_ID, CREATED_AT) VALUES (?,?,?)";

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
                    .createNativeQuery(INSERT_IGNORE)
                    .setParameter(1, announceImage.getImageUrl())
                    .setParameter(2, announceImage.getAnnounce().getAnnounceId())
                    .setParameter(3, LocalDateTime.now().toString())
                    .executeUpdate();
        }
    }
}
