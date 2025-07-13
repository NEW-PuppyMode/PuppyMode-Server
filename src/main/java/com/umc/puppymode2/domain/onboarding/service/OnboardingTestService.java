package com.umc.puppymode2.domain.onboarding.service;

import com.umc.puppymode2.domain.onboarding.dto.OnboardingTestAnswerDTO;
import com.umc.puppymode2.domain.onboarding.dto.OnboardingTestReqDTO;
import com.umc.puppymode2.domain.onboarding.dto.OnboardingTestResDTO;
import com.umc.puppymode2.domain.onboarding.entity.Puppy;
import com.umc.puppymode2.domain.onboarding.entity.PuppyLevel;
import com.umc.puppymode2.domain.onboarding.entity.PuppyType;
import com.umc.puppymode2.domain.onboarding.repository.PuppyLevelRepository;
import com.umc.puppymode2.domain.onboarding.repository.PuppyRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.handler.TempHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OnboardingTestService {

    private final PuppyRepository puppyRepository;
    private final PuppyLevelRepository puppyLevelRepository;

    public OnboardingTestResDTO recommendAndCreatePuppy(OnboardingTestReqDTO onboardingTestReqDTO) {

        int eScore = 0, iScore = 0, fScore = 0, tScore = 0; // 각 유형 점수에 해당하는 변수 초기화

        // 모든 답변을 탐색하며 해당하는 유형의 점수를 업데이트
        for (OnboardingTestAnswerDTO answer : onboardingTestReqDTO.answers()) {

            int q = answer.questionId();
            int a = answer.answer();

            switch (q) {
                case 1, 2 -> { // E or I
                    if (a == 1) eScore++;
                    else iScore++;
                }
                case 5 -> { // E/F or I/T
                    if (a == 1) {
                        eScore++; fScore++;
                    } else {
                        iScore++; tScore++;
                    }
                }
                case 3, 4, 6 -> { // F or T
                    if (a == 1) fScore++;
                    else tScore++;
                }
                default -> throw new TempHandler(ErrorStatus.INVALID_QUESTION_ID);
            }
        }

        // 두 유형 중 어느쪽에 해당하는지 판별 후 강아지 종 매칭
        String eOrI = (eScore >= iScore) ? "E" : "I";
        String fOrT = (fScore >= tScore) ? "F" : "T";
        PuppyType type = getDogTypeByTrait(eOrI, fOrT);

        // 해당 강아지 타입의 Level 1 찾기
        PuppyLevel level1 = puppyLevelRepository.findByPuppyTypeAndPuppyLevel(type, 1)
                .orElseThrow(() -> new TempHandler(ErrorStatus.PUPPY_LEVEL_NOT_FOUND));

        // Puppy 객체 생성 및 저장
        Puppy puppy = Puppy.builder()
//                .user(user)
                .puppyLevel(level1)
                .puppyName(level1.getPuppyType().getBreed())
                .puppyExp(0)
                .build();
        puppyRepository.save(puppy);

        // 온보딩 화면에 검사 결과로 표시될 데이터 DTO 생성 및 반환
        return new OnboardingTestResDTO(
                type.getType(),
                type.getBreed(),
                type.getDescription(),
                type.getImageUrl()
        );
    }

    // 도출된 유형에 매칭되는 강아지 종 반환
    private PuppyType getDogTypeByTrait(String eOrI, String fOrT) {
        return switch (eOrI + fOrT) {
            case "EF" -> PuppyType.BICHON;
            case "IF" -> PuppyType.SHIBA;
            case "ET" -> PuppyType.CORGI;
            case "IT" -> PuppyType.POODLE;
            default -> throw new TempHandler(ErrorStatus.INVALID_TRAIT_COMBINATION);
        };
    }
}
