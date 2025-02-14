package com.jnu.ticketdomain.AnnounceImage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnu.ticketdomain.AnnounceImage.config.TestDataSourceConfig;
import com.jnu.ticketdomain.domains.announce.domain.Announce;
import com.jnu.ticketdomain.domains.announce.domain.AnnounceImage;
import com.jnu.ticketdomain.domains.announce.repository.AnnounceImageNativeRepository;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ComponentScan(basePackages = {"com.jnu.ticketdomain.domains.announce.repository"})
@ActiveProfiles("test-mysql")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestDataSourceConfig.class)
public class AnnounceImageNativeRepositoryTest {

    @Autowired private AnnounceImageNativeRepository announceImageNativeRepository;

    @Autowired private DataSource dataSource;

    @PersistenceContext private EntityManager entityManager;

    private Announce announce;

    @Nested
    @DisplayName("이미지 수정 메서드의 쿼리를 테스트한다.")
    class ImageUpdateUnitTest {

        @BeforeEach
        public void setUpImages() throws Exception {

            // DB 연결정보 출력
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                System.out.println("Database Product Name: " + metaData.getDatabaseProductName());
                System.out.println("Database Product Version: " + metaData.getDatabaseProductVersion());
                System.out.println("Database URL: " + metaData.getURL());
                System.out.println("Database User: " + metaData.getUserName());
            }

            announce = Announce.builder().announceTitle("example").build();
            entityManager.persist(announce);
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
        void shouldDeleteNotFoundImage() {
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

        @Test
        @DisplayName("중복된 원소인 경우 무시하고 삽입한다.")
        void ignoreInsertTest() {
            AnnounceImage imageToInsert1 =
                    AnnounceImage.builder()
                            .imageUrl("http://example.com/image1.jpg")
                            .announce(announce)
                            .build();
            AnnounceImage imageToInsert2 =
                    AnnounceImage.builder()
                            .imageUrl("http://example.com/image3.jpg")
                            .announce(announce)
                            .build();
            announceImageNativeRepository.saveAllDuplicateOn(
                    Arrays.asList(imageToInsert1, imageToInsert2));

            List<AnnounceImage> remainingImages =
                    entityManager
                            .createQuery("SELECT i FROM AnnounceImage i", AnnounceImage.class)
                            .getResultList();

            assertThat(remainingImages).hasSize(3);
            for (int i = 0; i < remainingImages.size(); i++) {
                assertThat(remainingImages.get(i).getImageUrl())
                        .isEqualTo("http://example.com/image" + (i + 1) + ".jpg");
            }
        }

        @Test
        @DisplayName("수정시 created_at이 잘 들어온다.")
        void insert_created_at_test() {
            // given
            AnnounceImage imageToInsert1 =
                    AnnounceImage.builder()
                            .imageUrl("http://example.com/image1.jpg")
                            .announce(announce)
                            .build();
            AnnounceImage imageToInsert2 =
                    AnnounceImage.builder()
                            .imageUrl("http://example.com/image3.jpg")
                            .announce(announce)
                            .build();
            announceImageNativeRepository.saveAllDuplicateOn(
                    Arrays.asList(imageToInsert1, imageToInsert2));

            List<AnnounceImage> remainingImages =
                    entityManager
                            .createQuery("SELECT i FROM AnnounceImage i", AnnounceImage.class)
                            .getResultList();

            for (AnnounceImage remainingImage : remainingImages) {
                assertThat(remainingImage.getCreatedAt()).isNotNull();
            }
        }
    }
}
