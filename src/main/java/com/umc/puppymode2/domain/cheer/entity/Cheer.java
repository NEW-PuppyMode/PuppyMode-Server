package com.umc.puppymode2.domain.cheer.entity;

import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import com.umc.puppymode2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 친구에게 보낸 응원.
 *
 * 응원은 (보낸 사람, 받는 사람, 응원 대상 음주 날짜)당 1회만 보낼 수 있다.
 * 더블탭이나 동시 요청이 들어와도 UNIQUE 제약이 마지막 방어선이 된다.
 *
 * <h4>시간 기준</h4>
 * expiresAt / readAt / createdAt은 모두 서버(JVM) 기본 타임존 기준의 LocalDateTime으로 저장한다.
 * createdAt은 BaseEntity의 JPA Auditing이 그렇게 채우므로, 같은 행의 다른 시각 컬럼도 같은 기준이어야
 * 서로 비교하거나 응답으로 내릴 때 어긋나지 않는다. (친구 요청 respondedAt과 같은 원칙)
 * "KST 자정에 만료"라는 의미는 만료 시각을 계산할 때만 KST로 계산한 뒤 기본 타임존 값으로 바꿔서 담는다.
 * 응답에 내릴 때는 FriendConverter.toKstOffset으로 KST(+09:00)로 변환한다.
 */
@Entity
@Table(
        name = "cheer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cheer_once", columnNames = {"sender_id", "receiver_id", "target_date"})
        },
        indexes = {
                // 받은 응원 목록 / 안 읽은 응원 수 조회용
                @Index(name = "idx_cheer_inbox", columnList = "receiver_id, expires_at"),
                // 만료 응원 정리 배치용
                @Index(name = "idx_cheer_expire", columnList = "expires_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cheer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cheer_id")
    private Long cheerId;

    // 응원을 보낸 사용자
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    // 응원을 받은 사용자
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    // 사용한 문구 템플릿. 이후 템플릿이 바뀌어도 아래 스냅샷으로 표시하므로 조회에는 쓰지 않는다.
    @Column(name = "cheer_template_id", nullable = false)
    private Long cheerTemplateId;

    // 발송 시점의 분류 스냅샷
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    private CheerCategory category;

    // 발송 시점의 문구 스냅샷
    @Column(name = "message_snapshot", nullable = false, length = 100)
    private String messageSnapshot;

    // 응원 대상 음주 날짜 (친구의 DrinkHistory.drink_date, KST 기준 날짜)
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    // 만료 시각. 이 시각 이후에는 받은 응원 목록에서 사라진다.
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 받은 사람이 읽은 시각. 안 읽었으면 null
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Cheer of(Long senderId, Long receiverId, CheerTemplate template,
                           LocalDate targetDate, LocalDateTime expiresAt) {
        Cheer cheer = new Cheer();
        cheer.senderId = senderId;
        cheer.receiverId = receiverId;
        cheer.cheerTemplateId = template.getCheerTemplateId();
        cheer.category = template.getCategory();
        cheer.messageSnapshot = template.getMessage();
        cheer.targetDate = targetDate;
        cheer.expiresAt = expiresAt;
        return cheer;
    }

    public boolean isRead() {
        return readAt != null;
    }
}
