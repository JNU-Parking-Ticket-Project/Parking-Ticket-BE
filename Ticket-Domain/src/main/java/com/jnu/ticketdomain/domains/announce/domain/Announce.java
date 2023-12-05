package com.jnu.ticketdomain.domains.announce.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 공지사항 Entity. 제목과 내용, 생성일 필드가 존재하며, String, String, LocalDateTime 타입이다.
 *
 * @author cookie
 * @version 1.0
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "announce_tb")
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
public class Announce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long announceId;

    @Column(name = "title", nullable = false)
    @ColumnDefault("'제목을 입력해주세요.'")
    private String announceTitle;

    @Column(name = "content", nullable = false)
    @ColumnDefault("'내용을 입력해주세요.'")
    private String announceContent;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Announce(String announceTitle, String announceContent){
        this.announceTitle=announceTitle;
        this.announceContent=announceContent;
    }

    public void updateTitle(String title){
        this.announceTitle = title;
    }

    public void updateContent(String content){
        this.announceContent = content;
    }
}
