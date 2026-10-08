package com.umc.puppymode2.domain.complaint.event;

import com.umc.puppymode2.domain.complaint.service.ComplaintSlackSender;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ComplaintEventListener {

    private final ComplaintSlackSender slackSender;

    // 커밋된 신고만 알린다. 알림 실패는 ComplaintSlackSender가 삼키므로 신고 접수에는 영향이 없다.
    @Async("complaintExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComplaintCreated(ComplaintCreatedEvent event) {
        slackSender.send(buildMessage(event), buildData(event));
    }

    // n8n에서 스프레드시트 열로 매핑할 구조화 필드. 값이 없는 항목(강아지 이름 등)은 보내지 않는다.
    static Map<String, Object> buildData(ComplaintCreatedEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("occurredAt", OffsetDateTime.now(ZoneId.of("Asia/Seoul")).withNano(0).toString());
        data.put("complaintId", event.complaintId());
        data.put("reason", event.reason().getLabel());
        data.put("targetUserId", event.targetUserId());
        data.put("targetUsername", event.targetUsername());
        data.put("targetPuppyName", event.targetPuppyName());
        data.put("reporterId", event.reporterId());
        return data;
    }

    static String buildMessage(ComplaintCreatedEvent event) {
        return "🚨 새 신고가 접수됐어요 (#" + event.complaintId() + ")\n"
                + "• 사유: " + event.reason().getLabel() + "\n"
                + "• 신고 대상: " + event.targetUsername() + " (userId " + event.targetUserId() + ")"
                + (event.targetPuppyName() == null ? "" : ", 강아지 " + event.targetPuppyName()) + "\n"
                + "• 신고자 userId: " + event.reporterId();
    }
}
