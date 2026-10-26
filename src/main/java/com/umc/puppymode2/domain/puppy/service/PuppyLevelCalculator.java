package com.umc.puppymode2.domain.puppy.service;

import com.umc.puppymode2.domain.puppy.entity.LevelExp;
import com.umc.puppymode2.domain.puppy.repository.LevelExpRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 경험치(exp) → 레벨 / 레벨 내 진행률(%) 변환.
 *
 * 메인 화면(GET /main)과 월간 리포트의 goalReward(before/after)가 같은 계산식을 쓰도록 한 곳에 모아둔다.
 */
@Component
@RequiredArgsConstructor
public class PuppyLevelCalculator {

    private static final int MAX_LEVEL = 30;

    private final LevelExpRepository levelExpRepository;

    public LevelProgress calculate(int exp) {
        LevelExp levelInfo = levelExpRepository.findByExp(exp)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEVEL_NOT_FOUND));
        return new LevelProgress(levelInfo.getLevel(), calculatePercent(exp, levelInfo));
    }

    // 레벨 단위 진행률 계산
    // 현재 레벨의 minExp ~ maxExp 구간 기준
    // 레벨 30 도달 시 100% 고정
    private int calculatePercent(int exp, LevelExp levelInfo) {
        if (levelInfo.getLevel() == MAX_LEVEL) return 100;

        int minExp = levelInfo.getMinExp();
        int maxExp = levelInfo.getMaxExp();

        double ratio = (double) (exp - minExp) / (maxExp - minExp);
        return (int) (ratio * 100);
    }

    public record LevelProgress(int level, int percent) {
    }
}
