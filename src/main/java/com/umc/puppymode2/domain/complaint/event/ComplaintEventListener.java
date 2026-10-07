package com.umc.puppymode2.domain.complaint.event;

import com.umc.puppymode2.domain.complaint.service.ComplaintSlackSender;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ComplaintEventListener {

    private final ComplaintSlackSender slackSender;

    // 커밋된 신고만 알린다. 알림 실패는 ComplaintSlackSender가 삼키므로 신고 접수에는 영향이 없다.
    @Async("complaintExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComplaintCreated(ComplaintCreatedEvent event) {
        slackSender.send(buildMessage(event));
    }

    static String buildMessage(ComplaintCreatedEvent event) {
        return "🚨 새 신고가 접수됐어요 (#" + event.complaintId() + ")\n"
                + "• 사유: " + event.reason().getLabel() + "\n"
                + "• 신고 대상: " + event.targetUsername() + " (userId " + event.targetUserId() + ")"
                + (event.targetPuppyName() == null ? "" : ", 강아지 " + event.targetPuppyName()) + "\n"
                + "• 신고자 userId: " + event.reporterId();
    }
}
