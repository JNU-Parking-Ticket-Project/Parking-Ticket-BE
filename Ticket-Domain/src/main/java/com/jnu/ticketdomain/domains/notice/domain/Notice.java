package com.jnu.ticketdomain.domains.notice.domain;

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

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "notice_tb")
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long noticeId;

    @Column(name = "content", nullable = false)
    @ColumnDefault("'내용을 입력해주세요.'")
    private String noticeContent;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    @CreatedDate
    private LocalDateTime modifiedAt;

    @Builder
    public Notice(String noticeContent){
        this.noticeContent=noticeContent;
    }

    public void updateContent(String content){
        this.noticeContent = content;
    }
}
