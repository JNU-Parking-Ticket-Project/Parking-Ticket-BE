package com.jnu.ticketdomain.AnnounceImage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import com.jnu.ticketdomain.domains.announce.repository.AnnounceImageNativeRepository;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ComponentScan(basePackages = "com.jnu.ticketdomain.domains.announce.repository")
@Transactional
public class AnnounceImageNativeRepositoryTest {

    @Autowired private AnnounceImageNativeRepository announceImageNativeRepository;

    @PersistenceContext private EntityManager entityManager;

    private Announce announce;

    @BeforeEach
    public void setUp() {
        announce = Announce.builder().build();
        entityManager.persist(announce);
    }

    @Nested
    class WhenDeleteNotFoundImages {

        @BeforeEach
        public void setUpImages() {
            AnnounceImage image1 =
                    AnnounceImage.builder()
                            .imageUrl("http://example.com/image1.jpg")
                            .announce(announce)
                            .build();
            entityManager.persist(image1);
            AnnounceImage image2 =
                    AnnounceImage.builder()
                            .imageUrl("http://example.com/image2.jpg")
                            .announce(announce)
                            .build();
            entityManager.persist(image2);
        }

        @Test
        @DisplayName("리스트에 들어있는 원소와 일치하지 않으면 삭제한다.")
        public void shouldDeleteNotFoundImage() {
            AnnounceImage imageToDelete =
                    AnnounceImage.builder().imageUrl("http://example.com/image1.jpg").build();
            AnnounceImage imageToDelete2 =
                    AnnounceImage.builder().imageUrl("http://example.com/image3.jpg").build();
            announceImageNativeRepository.deleteNotFoundImage(
                    Arrays.asList(imageToDelete, imageToDelete2));

            List<AnnounceImage> remainingImages =
                    entityManager
                            .createQuery("SELECT i FROM AnnounceImage i", AnnounceImage.class)
                            .getResultList();

            assertThat(remainingImages).hasSize(1);
            assertThat(remainingImages.get(0).getImageUrl())
                    .isEqualTo("http://example.com/image1.jpg");
        }
    }
}
