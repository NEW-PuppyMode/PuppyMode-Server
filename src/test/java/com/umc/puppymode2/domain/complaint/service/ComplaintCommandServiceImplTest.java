package com.umc.puppymode2.domain.complaint.service;

import com.umc.puppymode2.domain.complaint.dto.ComplaintCreateRequestDTO;
import com.umc.puppymode2.domain.complaint.dto.ComplaintCreateResponseDTO;
import com.umc.puppymode2.domain.complaint.entity.UserComplaint;
import com.umc.puppymode2.domain.complaint.entity.enums.ComplaintReason;
import com.umc.puppymode2.domain.complaint.entity.enums.ComplaintStatus;
import com.umc.puppymode2.domain.complaint.event.ComplaintCreatedEvent;
import com.umc.puppymode2.domain.complaint.exception.ComplaintErrorStatus;
import com.umc.puppymode2.domain.complaint.repository.UserComplaintRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.auth.enums.Provider;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintCommandServiceImplTest {

    @Mock
    private UserComplaintRepository complaintRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PuppyRepository puppyRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ComplaintCommandServiceImpl service;

    private final Long myUserId = 1L;
    private final Long targetUserId = 55L;

    private User user(Long id, String name, UserStatus status) {
        User user = User.builder()
                .username(name)
                .email("user" + id + "@test.com")
                .provider(Provider.KAKAO)
                .status(status)
                .build();
        ReflectionTestUtils.setField(user, "userId", id);
        return user;
    }

    private Puppy puppy(String name) {
        return Puppy.builder().puppyName(name).build();
    }

    private ComplaintCreateRequestDTO request(ComplaintReason reason, String detail) {
        return new ComplaintCreateRequestDTO(targetUserId, reason, detail);
    }

    private void givenTargetExists() {
        when(userRepository.findById(targetUserId))
                .thenReturn(Optional.of(user(targetUserId, "정우주", UserStatus.NORMAL)));
    }

    private void givenSaveAssignsId(Long id) {
        when(complaintRepository.saveAndFlush(any(UserComplaint.class))).thenAnswer(inv -> {
            UserComplaint complaint = inv.getArgument(0);
            ReflectionTestUtils.setField(complaint, "userComplaintId", id);
            return complaint;
        });
    }

    private void assertError(ComplaintErrorStatus expected, Runnable action) {
        GeneralException e = assertThrows(GeneralException.class, action::run);
        assertEquals(expected.getCode(), e.getErrorReason().getCode());
    }

    @Test
    void 신고하면_접수_상태로_저장하고_대상_이름과_강아지_이름을_스냅샷으로_남긴다() {
        givenTargetExists();
        when(puppyRepository.findByUser_UserId(targetUserId)).thenReturn(Optional.of(puppy("별이")));
        givenSaveAssignsId(7L);

        ComplaintCreateResponseDTO result = service.createComplaint(myUserId, request(ComplaintReason.ABUSE_HARASSMENT, null));

        assertEquals(7L, result.getComplaintId());
        ArgumentCaptor<UserComplaint> saved = ArgumentCaptor.forClass(UserComplaint.class);
        verify(complaintRepository).saveAndFlush(saved.capture());
        assertEquals(myUserId, saved.getValue().getReporterId());
        assertEquals(targetUserId, saved.getValue().getTargetUserId());
        assertEquals(ComplaintReason.ABUSE_HARASSMENT, saved.getValue().getReason());
        assertEquals(ComplaintStatus.RECEIVED, saved.getValue().getStatus());
        assertEquals("정우주", saved.getValue().getTargetUsername());
        assertEquals("별이", saved.getValue().getTargetPuppyName());
    }

    @Test
    void 신고가_저장되면_접수_이벤트를_발행한다() {
        givenTargetExists();
        when(puppyRepository.findByUser_UserId(targetUserId)).thenReturn(Optional.of(puppy("별이")));
        givenSaveAssignsId(7L);

        service.createComplaint(myUserId, request(ComplaintReason.SPAM_AD, null));

        ArgumentCaptor<ComplaintCreatedEvent> event = ArgumentCaptor.forClass(ComplaintCreatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(7L, event.getValue().complaintId());
        assertEquals(myUserId, event.getValue().reporterId());
        assertEquals(targetUserId, event.getValue().targetUserId());
        assertEquals("정우주", event.getValue().targetUsername());
        assertEquals("별이", event.getValue().targetPuppyName());
        assertEquals(ComplaintReason.SPAM_AD, event.getValue().reason());
    }

    @Test
    void 강아지가_없는_대상도_신고할_수_있고_강아지_이름은_null로_남긴다() {
        givenTargetExists();
        when(puppyRepository.findByUser_UserId(targetUserId)).thenReturn(Optional.empty());
        givenSaveAssignsId(7L);

        service.createComplaint(myUserId, request(ComplaintReason.INAPPROPRIATE_NICKNAME, null));

        ArgumentCaptor<UserComplaint> saved = ArgumentCaptor.forClass(UserComplaint.class);
        verify(complaintRepository).saveAndFlush(saved.capture());
        assertNull(saved.getValue().getTargetPuppyName());
    }

    @Test
    void 자기_자신은_신고할_수_없다() {
        assertError(ComplaintErrorStatus.CANNOT_COMPLAIN_SELF,
                () -> service.createComplaint(myUserId, new ComplaintCreateRequestDTO(myUserId, ComplaintReason.SPAM_AD, null)));

        verify(complaintRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void 기타_사유인데_직접_입력이_비어_있으면_거절한다() {
        assertError(ComplaintErrorStatus.DETAIL_REQUIRED,
                () -> service.createComplaint(myUserId, request(ComplaintReason.OTHER, null)));
        assertError(ComplaintErrorStatus.DETAIL_REQUIRED,
                () -> service.createComplaint(myUserId, request(ComplaintReason.OTHER, "   ")));

        verify(complaintRepository, never()).saveAndFlush(any());
    }

    @Test
    void 기타_사유는_직접_입력_내용을_저장한다() {
        givenTargetExists();
        when(puppyRepository.findByUser_UserId(targetUserId)).thenReturn(Optional.empty());
        givenSaveAssignsId(7L);

        service.createComplaint(myUserId, request(ComplaintReason.OTHER, "프로필 사진이 불쾌해요"));

        ArgumentCaptor<UserComplaint> saved = ArgumentCaptor.forClass(UserComplaint.class);
        verify(complaintRepository).saveAndFlush(saved.capture());
        assertEquals("프로필 사진이 불쾌해요", saved.getValue().getDetail());
    }

    @Test
    void 기타가_아닌_사유에_직접_입력이_있어도_저장하지_않는다() {
        givenTargetExists();
        when(puppyRepository.findByUser_UserId(targetUserId)).thenReturn(Optional.empty());
        givenSaveAssignsId(7L);

        service.createComplaint(myUserId, request(ComplaintReason.SPAM_AD, "무시되어야 하는 내용"));

        ArgumentCaptor<UserComplaint> saved = ArgumentCaptor.forClass(UserComplaint.class);
        verify(complaintRepository).saveAndFlush(saved.capture());
        assertNull(saved.getValue().getDetail());
    }

    @Test
    void 없는_사용자는_신고할_수_없다() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertError(ComplaintErrorStatus.TARGET_USER_NOT_FOUND,
                () -> service.createComplaint(myUserId, request(ComplaintReason.SPAM_AD, null)));

        verify(complaintRepository, never()).saveAndFlush(any());
    }

    @Test
    void NORMAL이_아닌_사용자는_없는_사용자로_취급한다() {
        when(userRepository.findById(targetUserId))
                .thenReturn(Optional.of(user(targetUserId, "탈퇴한사람", UserStatus.STOP)));

        assertError(ComplaintErrorStatus.TARGET_USER_NOT_FOUND,
                () -> service.createComplaint(myUserId, request(ComplaintReason.SPAM_AD, null)));
    }

    @Test
    void 같은_대상을_같은_사유로_다시_신고하면_409() {
        givenTargetExists();
        when(complaintRepository.existsByReporterIdAndTargetUserIdAndReason(
                myUserId, targetUserId, ComplaintReason.SPAM_AD)).thenReturn(true);

        assertError(ComplaintErrorStatus.ALREADY_COMPLAINED,
                () -> service.createComplaint(myUserId, request(ComplaintReason.SPAM_AD, null)));

        verify(complaintRepository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void 같은_대상이라도_다른_사유로는_신고할_수_있다() {
        givenTargetExists();
        when(complaintRepository.existsByReporterIdAndTargetUserIdAndReason(
                myUserId, targetUserId, ComplaintReason.SPAM_AD)).thenReturn(false);
        when(puppyRepository.findByUser_UserId(targetUserId)).thenReturn(Optional.empty());
        givenSaveAssignsId(8L);

        ComplaintCreateResponseDTO result = service.createComplaint(myUserId, request(ComplaintReason.SPAM_AD, null));

        assertEquals(8L, result.getComplaintId());
    }

    @Test
    void 동시_요청으로_UNIQUE에_걸리면_이미_신고한_것으로_409이고_알림은_보내지_않는다() {
        givenTargetExists();
        when(puppyRepository.findByUser_UserId(targetUserId)).thenReturn(Optional.empty());
        when(complaintRepository.saveAndFlush(any(UserComplaint.class)))
                .thenThrow(new DataIntegrityViolationException("uk_user_complaint_pair_reason"));

        assertError(ComplaintErrorStatus.ALREADY_COMPLAINED,
                () -> service.createComplaint(myUserId, request(ComplaintReason.SPAM_AD, null)));

        verify(eventPublisher, never()).publishEvent(any());
    }
}
