package com.umc.puppymode2.domain.goal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_goal_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        // 한 유저는 한 달에 목표 하나만 생성 가능
                        name = "uk_user_goal_month",
                        columnNames = {"user_id", "goal_month"}
                )
        },
        indexes = {
                @Index(name = "idx_user_goal_user", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGoalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    private Long goalId;

    // FK 컬럼 명확히 지정
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 목표가 적용되는 월 (항상 해당 월의 1일 저장)
    @Column(name = "goal_month", nullable = false)
    private LocalDate goalMonth;

    // 이번 달 목표 음주 횟수
    @Column(name = "monthly_goal_count", nullable = false)
    private Integer monthlyGoalCount;

    // 목표 설정 시간
    @Column(name = "goal_set_at", nullable = false)
    private LocalDateTime goalSetAt;

    // 마지막 GPT 코멘트 전송 시간
    @Column(name = "last_gpt_comment_sent_at")
    private LocalDateTime lastGptCommentSentAt;

    // 생성 시간
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 수정 시간
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 월간 목표 달성 경험치 지급 여부
    @Column(name = "is_rewarded", nullable = false)
    private boolean rewarded = false;

    // 목표 달성 경험치 지급 처리
    public void markRewarded() {
        this.rewarded = true;
    }
}