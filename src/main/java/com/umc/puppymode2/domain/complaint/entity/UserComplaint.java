package com.umc.puppymode2.domain.complaint.entity;

import com.umc.puppymode2.domain.common.BaseEntity;
import com.umc.puppymode2.domain.complaint.entity.enums.ComplaintReason;
import com.umc.puppymode2.domain.complaint.entity.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 신고. (월간 리포트용 Report와 구분하기 위해 Complaint로 이름을 통일했다)
 *
 * (reporter_id, target_user_id, reason) 쌍당 행은 최대 1개다. 같은 대상을 같은 사유로 다시 신고할 수 없고,
 * 다른 사유로는 신고할 수 있다. 사유가 OTHER인 경우도 한 번만 신고할 수 있다.
 * 신고 시점의 대상 이름·강아지 이름은 나중에 바뀌어도 신고 내용을 확인할 수 있도록 스냅샷으로 저장한다.
 */
@Entity
@Table(
        name = "user_complaint",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_complaint_pair_reason",
                        columnNames = {"reporter_id", "target_user_id", "reason"})
        },
        indexes = {
                @Index(name = "idx_user_complaint_target", columnList = "target_user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserComplaint extends BaseEntity {

    public static final int DETAIL_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_complaint_id")
    private Long userComplaintId;

    // 신고한 사용자
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    // 신고당한 사용자
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private ComplaintReason reason;

    // 사유가 OTHER일 때 직접 입력한 내용. 그 외에는 null
    @Column(name = "detail", length = DETAIL_MAX_LENGTH)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ComplaintStatus status;

    // 신고 시점의 대상 정보 스냅샷
    @Column(name = "target_username")
    private String targetUsername;

    @Column(name = "target_puppy_name")
    private String targetPuppyName;

    public static UserComplaint create(Long reporterId, Long targetUserId, ComplaintReason reason, String detail,
                                       String targetUsername, String targetPuppyName) {
        if (reporterId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신은 신고할 수 없습니다.");
        }
        UserComplaint complaint = new UserComplaint();
        complaint.reporterId = reporterId;
        complaint.targetUserId = targetUserId;
        complaint.reason = reason;
        complaint.detail = detail;
        complaint.status = ComplaintStatus.RECEIVED;
        complaint.targetUsername = targetUsername;
        complaint.targetPuppyName = targetPuppyName;
        return complaint;
    }
}
