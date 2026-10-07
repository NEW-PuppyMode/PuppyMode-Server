package com.umc.puppymode2.domain.complaint.event;

import com.umc.puppymode2.domain.complaint.entity.enums.ComplaintReason;
import com.umc.puppymode2.domain.complaint.service.ComplaintSlackSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ComplaintEventListenerTest {

    @Mock
    private ComplaintSlackSender slackSender;

    @InjectMocks
    private ComplaintEventListener listener;

    @Test
    void 신고가_접수되면_사유_대상_신고자가_담긴_메시지를_Slack으로_보낸다() {
        listener.onComplaintCreated(new ComplaintCreatedEvent(
                7L, 1L, 55L, "정우주", "별이", ComplaintReason.ABUSE_HARASSMENT));

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(slackSender).send(text.capture());
        assertTrue(text.getValue().contains("#7"));
        assertTrue(text.getValue().contains("욕설·괴롭힘"));
        assertTrue(text.getValue().contains("정우주"));
        assertTrue(text.getValue().contains("userId 55"));
        assertTrue(text.getValue().contains("별이"));
        assertTrue(text.getValue().contains("신고자 userId: 1"));
    }

    @Test
    void 강아지_이름이_없으면_강아지_항목을_빼고_보낸다() {
        String message = ComplaintEventListener.buildMessage(new ComplaintCreatedEvent(
                7L, 1L, 55L, "정우주", null, ComplaintReason.SPAM_AD));

        assertFalse(message.contains("강아지"));
        assertFalse(message.contains("null"));
    }
}
