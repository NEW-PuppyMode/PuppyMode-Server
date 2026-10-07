package com.umc.puppymode2.domain.complaint.dto;

import com.umc.puppymode2.domain.complaint.entity.UserComplaint;
import com.umc.puppymode2.domain.complaint.entity.enums.ComplaintReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintCreateRequestDTO {

    @NotNull(message = "신고할 사용자를 확인해주세요")
    private Long userId;

    @NotNull(message = "신고 사유를 선택해주세요")
    private ComplaintReason reason;

    // 사유가 OTHER일 때만 필수. 필수 여부는 서비스에서 COMPLAINT4002로 검증한다.
    @Size(max = UserComplaint.DETAIL_MAX_LENGTH, message = "신고 내용은 500자 이하로 입력해주세요")
    private String detail;

    // Jackson이 역직렬화 때 이 setter를 사용하므로, 검증(@Size) 전에 앞뒤 공백이 제거된다.
    public void setDetail(String detail) {
        this.detail = detail == null ? null : detail.trim();
    }
}
