package com.jnu.ticketdomain.domains.announce.domain;


import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "announce_image_tb")
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
public class AnnounceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long announceImageId;

    @Column(name = "url", nullable = false, unique = true)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announce_id")
    private Announce announce;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "bit(1) default 0")
    private boolean isDeleted;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public AnnounceImage(String imageUrl, Announce announce, LocalDateTime createdAt) {
        this.imageUrl = imageUrl;
        this.announce = announce;
        this.createdAt= createdAt;
    }
}
