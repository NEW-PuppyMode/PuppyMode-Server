package com.umc.puppymode2.domain.goal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_goal_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGoalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long goalId;

    private Long userId;

    private Integer monthlyGoalCount;  // 이번 달 목표 음주 횟수

    private Long monthlyActualCount; // 이번 달 실제 음주 횟수

    private Boolean isGoalExceeded;

    private LocalDateTime goalSetAt;

    private LocalDateTime lastGptCommentSentAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}