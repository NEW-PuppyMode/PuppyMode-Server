package com.umc.puppymode2.domain.cheer.repository;

import java.time.LocalDate;

// "내가 이 친구의 이 날짜 음주에 응원을 보냈는가"를 판정하기 위한 최소 컬럼 프로젝션
public interface CheerSentProjection {
    Long getReceiverId();
    LocalDate getTargetDate();
}
