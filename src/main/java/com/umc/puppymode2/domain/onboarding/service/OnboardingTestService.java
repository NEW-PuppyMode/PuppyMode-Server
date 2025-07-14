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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OnboardingTestService {

    private final PuppyRepository puppyRepository;
    private final PuppyLevelRepository puppyLevelRepository;

    public OnboardingTestResDTO recommendAndCreatePuppy(OnboardingTestReqDTO onboardingTestReqDTO) {

        // 올바른 reqDTO 형식인지 검증 후, 각 유형 점수에 해당하는 변수 초기화
        validOnboardingTestReqDTO(onboardingTestReqDTO);
        int eScore = 0, iScore = 0, fScore = 0, tScore = 0;

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

    // 올바른 답변 형식인지 검증
    private void validOnboardingTestReqDTO(OnboardingTestReqDTO onboardingTestReqDTO) {

        Set<Integer> questionIds = new HashSet<>();
        for (OnboardingTestAnswerDTO answer : onboardingTestReqDTO.answers()) {
            // 중복된 questionId가 있을 경우
            if (!questionIds.add(answer.questionId())) {
                throw new TempHandler(ErrorStatus.DUPLICATE_QUESTION_ID);
            }
        }

        // 모든 질문 번호(1~6)가 포함되어 있는지 체크
        Set<Integer> expectedIds = Set.of(1, 2, 3, 4, 5, 6);
        if (!questionIds.equals(expectedIds)) {
            throw new TempHandler(ErrorStatus.MISSING_QUESTION_IDS);
        }
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
