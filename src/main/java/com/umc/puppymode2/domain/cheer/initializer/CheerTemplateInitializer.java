package com.umc.puppymode2.domain.cheer.initializer;

import com.umc.puppymode2.domain.cheer.entity.CheerTemplate;
import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import com.umc.puppymode2.domain.cheer.repository.CheerTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 응원 문구 시드 데이터 등록.
 *
 * 이 프로젝트는 스키마를 ddl-auto: update로 관리하고 SQL 초기화 스크립트를 쓰지 않으므로,
 * 서버 시작 시 문구 테이블이 비어 있으면 기본 문구를 넣는다.
 *
 * 테이블에 한 건이라도 있으면 아무것도 하지 않는다. 운영 중에 DB에서 문구를 수정/비활성화해도
 * 재시작할 때 원래 문구가 다시 들어오지 않게 하기 위해서다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheerTemplateInitializer implements ApplicationRunner {

    private final CheerTemplateRepository cheerTemplateRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (cheerTemplateRepository.count() > 0) {
            return;
        }
        List<CheerTemplate> seeds = buildSeeds();
        try {
            cheerTemplateRepository.saveAll(seeds);
            log.info("[CHEER TEMPLATE] 기본 응원 문구 {}건을 등록했습니다.", seeds.size());
        } catch (DataIntegrityViolationException e) {
            // 여러 인스턴스가 동시에 처음 뜨면 UNIQUE(category, message)에 걸릴 수 있다.
            // 이 경우는 다른 인스턴스가 이미 같은 문구를 넣은 것이므로 정상으로 보고 넘어간다.
            log.info("[CHEER TEMPLATE] 다른 인스턴스가 이미 기본 응원 문구를 등록했습니다: {}", e.getMessage());
        }
        // 그 외 예외(DB 장애 등)는 삼키지 않고 그대로 던진다.
        // 삼키면 시드가 빈 채로 서버가 떠서, 재시작이나 수동 복구 전까지 응원 문구 조회가 비고 응원 보내기가 전부 실패한다.
        // 던지면 기동이 실패해 배포 환경이 재시작하면서 다시 시도된다.
    }

    List<CheerTemplate> buildSeeds() {
        List<CheerTemplate> seeds = new ArrayList<>();
        addAll(seeds, CheerCategory.PRANK,
                "이정도면 알콜 중독이야",
                "매일 마시면 그거는 병이야"
                // TODO: 장난 문구 3개("간이 남아…", "병원 가서 검…", "너 이러다…")는 설계서에서 문장이 잘려 있다.
                //       기획 확정본을 받으면 여기에 추가한다. (문구 테이블이 이미 채워진 환경에는 DB에 직접 INSERT 필요)
        );
        addAll(seeds, CheerCategory.COMFORT,
                "그래 마실 수도 있지",
                "너무 자책하지마",
                "다 이유가 있겠지",
                "힘든 일 있었구나",
                "마시고 풀렸으면 됐어");
        addAll(seeds, CheerCategory.CHEER,
                "그럴 수도 있어 다음이 중요해",
                "실수는 있을 수 있지 화이팅",
                "다음엔 꼭 참아보자!",
                "마신 건 마신거고 다음이 중요해",
                "완벽할 순 없지 다시 해보자");
        return seeds;
    }

    // 문구는 넘겨준 순서대로 display_order 1, 2, 3 ... 을 부여한다.
    private void addAll(List<CheerTemplate> seeds, CheerCategory category, String... messages) {
        for (int i = 0; i < messages.length; i++) {
            seeds.add(CheerTemplate.of(category, messages[i], i + 1));
        }
    }
}
