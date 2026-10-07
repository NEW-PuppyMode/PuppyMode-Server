package com.umc.puppymode2.domain.complaint.service;

import com.umc.puppymode2.domain.complaint.dto.ComplaintCreateRequestDTO;
import com.umc.puppymode2.domain.complaint.dto.ComplaintCreateResponseDTO;
import com.umc.puppymode2.domain.complaint.entity.UserComplaint;
import com.umc.puppymode2.domain.complaint.entity.enums.ComplaintReason;
import com.umc.puppymode2.domain.complaint.event.ComplaintCreatedEvent;
import com.umc.puppymode2.domain.complaint.exception.ComplaintErrorStatus;
import com.umc.puppymode2.domain.complaint.repository.UserComplaintRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComplaintCommandServiceImpl implements ComplaintCommandService {

    private final UserComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final PuppyRepository puppyRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public ComplaintCreateResponseDTO createComplaint(Long myUserId, ComplaintCreateRequestDTO request) {
        Long targetUserId = request.getUserId();
        ComplaintReason reason = request.getReason();

        // 1. 자기 자신은 신고할 수 없다.
        if (myUserId.equals(targetUserId)) {
            throw new GeneralException(ComplaintErrorStatus.CANNOT_COMPLAIN_SELF);
        }

        // 2. 사유가 기타(OTHER)면 직접 입력 내용이 필수이고, 그 외 사유에서는 입력 내용을 저장하지 않는다.
        String detail = normalizeDetail(reason, request.getDetail());

        // 3. 신고 대상은 존재하고 NORMAL 이어야 한다. (탈퇴/휴면 사용자는 없는 사용자로 취급)
        User target = userRepository.findById(targetUserId)
                .filter(user -> user.getStatus() == UserStatus.NORMAL)
                .orElseThrow(() -> new GeneralException(ComplaintErrorStatus.TARGET_USER_NOT_FOUND));

        // 4. 같은 대상에게 같은 사유로 이미 신고했다면 409. 먼저 확인해 불필요한 INSERT 실패를 피하고,
        //    동시 요청은 아래 UNIQUE 위반 처리가 최종적으로 막는다.
        if (complaintRepository.existsByReporterIdAndTargetUserIdAndReason(myUserId, targetUserId, reason)) {
            throw new GeneralException(ComplaintErrorStatus.ALREADY_COMPLAINED);
        }

        // 5. 신고 시점의 대상 이름·강아지 이름을 스냅샷으로 저장한다. 강아지가 없으면(온보딩 미완료) null이다.
        String puppyName = puppyRepository.findByUser_UserId(targetUserId)
                .map(Puppy::getPuppyName)
                .orElse(null);

        try {
            UserComplaint saved = complaintRepository.saveAndFlush(
                    UserComplaint.create(myUserId, targetUserId, reason, detail, target.getUsername(), puppyName));
            eventPublisher.publishEvent(new ComplaintCreatedEvent(
                    saved.getUserComplaintId(), myUserId, targetUserId, target.getUsername(), puppyName, reason));
            return ComplaintCreateResponseDTO.builder().complaintId(saved.getUserComplaintId()).build();
        } catch (DataIntegrityViolationException e) {
            // 더블탭·동시 요청으로 UNIQUE(reporter_id, target_user_id, reason)에 걸린 경우
            throw new GeneralException(ComplaintErrorStatus.ALREADY_COMPLAINED);
        }
    }

    private String normalizeDetail(ComplaintReason reason, String detail) {
        if (reason != ComplaintReason.OTHER) {
            return null;
        }
        if (detail == null || detail.isBlank()) {
            throw new GeneralException(ComplaintErrorStatus.DETAIL_REQUIRED);
        }
        return detail;
    }
}
