package com.umc.puppymode2.domain.onboarding.progress;

import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 온보딩(강아지 이름/ 내 이름 /최초 목표 설정) 완료 여부를 판단하는 서비스
 *
 * 별도의 진행 상태 컬럼을 두지 않고, 매 조회 시점에 실제 데이터
 * (Puppy.isCustomName, User.isCustomName, UserGoalHistory 존재 여부)를 조합해 계산함.
 * 각 단계 저장 API가 어떤 순서로 호출되든 항상 정확한 완료 여부 반환 가능
 */
@Service
@RequiredArgsConstructor
public class OnboardingProgressService {

    private final PuppyRepository puppyRepository;
    private final UserGoalHistoryRepository userGoalHistoryRepository;

    /**
     * 온보딩(강아지이름 + 내이름 + 최초 목표 설정) 완료 여부 계산
     *
     * 강아지 유형 검사(Puppy 생성) 자체는 판단 범위에 포함 x
     * 검사 완료 여부는 기존 AuthMeResponseDTO.isOnboarded()로 별도 확인한다.
     *
     * @param user 조회 대상 유저
     * @return 강아지 이름/ 내 이름/ 목표 3단계가 모두 완료됐으면 true
     */
    @Transactional(readOnly = true)
    public boolean isOnboardingCompleted(User user) {
        Puppy puppy = puppyRepository.findByUser_UserId(user.getUserId()).orElse(null);

        if (puppy == null || !puppy.isCustomName()) {
            return false;
        }
        if (!user.isCustomName()) {
            return false;
        }
        return userGoalHistoryRepository.existsByUserId(user.getUserId());
    }
}
