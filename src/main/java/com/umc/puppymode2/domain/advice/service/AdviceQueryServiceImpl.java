package com.umc.puppymode2.domain.advice.service;

import com.umc.puppymode2.domain.advice.dto.AdviceResponseDTO;
import com.umc.puppymode2.domain.drinkhistory.repository.DrinkHistoryRepository;
import com.umc.puppymode2.domain.goal.entity.UserGoalHistory;
import com.umc.puppymode2.domain.goal.repository.UserGoalHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdviceQueryServiceImpl implements AdviceQueryService {

    private final UserGoalHistoryRepository userGoalHistoryRepository;
    private final DrinkHistoryRepository drinkHistoryRepository;
    private final Random random = new Random();

    @Override
    public AdviceResponseDTO getAdvice(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.withDayOfMonth(1);
        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate yesterday = today.minusDays(1);

        // 목표 횟수 가져오기
        int goal = userGoalHistoryRepository.findTopByUserIdOrderByGoalSetAtDesc(userId)
                .map(UserGoalHistory::getMonthlyGoalCount)
                .orElse(15); // 디폴트값 15

        // 이번 달 실제 음주 기록 횟수
        long actual = drinkHistoryRepository.countByUserUserIdAndDrinkDateBetween(userId, firstDay, lastDay);

        // 어제 기록 여부
        boolean recordedYesterday = drinkHistoryRepository.existsByUserUserIdAndDrinkDate(userId, yesterday);

        // 조언 메시지 생성
        String advice = getRandomAdvice(goal, actual, recordedYesterday);
        return new AdviceResponseDTO(advice);
    }

    private String getRandomAdvice(long goal, long actual, boolean recordedYesterday) {
        List<String> messages = new ArrayList<>();

        // 일반 메시지
        messages.addAll(List.of(
                "조금씩 나아지고 있어요. 지금처럼만 해봐요.",
                "오늘은 잘 해냈어요. 내일도 함께 해봐요.",
                "목표 지켜가는 모습이 보기 좋아요. 계속 이어가봐요.",
                "어제보다 오늘이 더 나았다면, 그걸로 충분해요.",
                "하루하루 잘 해내고 있어요. 그걸 잊지 말아봐요.",
                "쉬운 길은 아니지만, 분명히 잘 가고 있어요.",
                "시간이 좀 걸려도 괜찮아요. 방향은 맞고 있어요.",
                "계획 지키는 거, 생각보다 더 어렵죠? 그래도 잘했어요.",
                "지금처럼만 해봐요. 분명 좋은 결과가 올 거예요.",
                "요즘 많이 힘들었죠. 그래서 그랬던 거, 이해해요.",
                "그 날은 어쩔 수 없었겠죠. 그런 날도 있는 거예요.",
                "기록보다 중요한 건 오늘의 마음이에요. 그걸 잘 알고 있죠.",
                "조금 벗어났다고 해서 모든 게 무너진 건 아니에요.",
                "잠깐 멈춘 것도 괜찮아요. 다시 걷는 게 더 중요해요.",
                "자책하지 마요. 충분히 잘하고 있어요.",
                "이런 날도 있어요. 괜찮아요, 정말로요.",
                "이건 실패가 아니라 잠깐 멈춘 거예요. 다음엔 다시 움직이면 돼요.",
                "천천히 해도 괜찮아요. 중요한 건 계속 가는 거예요.",
                "지금도 충분히 잘하고 있어요. 나는 그렇게 생각해요.",
                "달력에 동그라미 너무 많아요. 이러다 술이랑 기념일 생기겠어요.",
                "아무래도 ‘술 마신 날’이 아니라 ‘안 마신 날’을 표시하는 게 빠르겠어요.",
                "기록만 열심히 하면 뭐해요. 숫자가 줄질 않아요.",
                "스스로한테 약속하신 거, 혹시 저만 기억하나요?",
                "다른 건 몰라도, 이쪽 루틴은 안 놓치시네요.",
                "오늘도 잘 지키셨다구요? 뭐야… 무서워졌어요.",
                "요즘 주인님 일관성 하나는 끝내줘요. 어쩜 매번 똑같을 수가 있죠?",
                "이건 실수인가요, 아니면 진짜 노력하신 건가요? 헷갈리네요.",
                "오랜만에 예측 틀렸네요. 오늘은 칭찬해드려야겠어요.",
                "이렇게 계획을 잘 지키는 건 처음 봐요. 뭐 잘못 드신 거 아니죠?"
        ));

        // 어제 기록했다면 이 문장 포함
        if (recordedYesterday) {
            messages.add("매일 기록 남기는 거, 그 자체로 충분히 대단해요.");
        }

        // 목표보다 초과했을 경우 냉정한 메시지 추가
        if (actual > goal) {
            messages.addAll(List.of(
                    String.format("이번 달 목표는 %d회였는데 %d회는 과했어요. 다음 달엔 조절해봐요.", goal, actual),
                    "줄이겠다는 말보다 줄이는 행동이 먼저 나와야 해요.",
                    "마시고 나서 후회한다면, 그건 이미 조절이 필요한 상태예요.",
                    "조금씩 괜찮다고 넘기다 보면, 어느새 무뎌질 수 있어요.",
                    "습관은 반복될수록 굳어져요. 이쯤에서 조정해봐요.",
                    "한두 번은 괜찮지만, 자주 그러면 경계선이 무너질 수 있어요.",
                    "이번 주는 계획보다 많이 벗어났어요. 조절이 필요해 보여요.",
                    "계획보다 많이 마셨다면, 그건 우연이 아니라 선택이에요",
                    "자주 마시는 건 괜찮은 게 아니라 익숙해진 거예요."
            ));
        }
        
        // 한 달에 10회 이상
        if(actual > 10) {
            messages.add("한 달 10회 이상은 위험선이에요. 이번엔 줄여봐요.");
        }

        // 랜덤 셔플 후 하나 선택
        Collections.shuffle(messages);
        return messages.get(random.nextInt(messages.size()));
    }
}
