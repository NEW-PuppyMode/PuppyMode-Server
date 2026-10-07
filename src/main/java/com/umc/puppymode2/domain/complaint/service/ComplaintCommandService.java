package com.umc.puppymode2.domain.complaint.service;

import com.umc.puppymode2.domain.complaint.dto.ComplaintCreateRequestDTO;
import com.umc.puppymode2.domain.complaint.dto.ComplaintCreateResponseDTO;

public interface ComplaintCommandService {

    // 사용자를 신고한다. 같은 대상을 같은 사유로 다시 신고할 수는 없다.
    ComplaintCreateResponseDTO createComplaint(Long myUserId, ComplaintCreateRequestDTO request);
}
