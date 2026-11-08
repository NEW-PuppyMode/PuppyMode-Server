package com.umc.puppymode2.domain.friend.cache;

import com.umc.puppymode2.domain.puppy.entity.LevelExp;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.LevelExpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 친구의 레벨을 계산하기 위한 메모리 캐시.
 *
 * 레벨은 Puppy에 저장된 값이 아니라 puppy_exp를 LevelExp 구간에 대입해서 구한다.
 * LevelExp는 거의 바뀌지 않는 작은 고정 테이블이라, 친구 N명마다 조회하거나 조인하지 않고
 * 한 번만 읽어 메모리에 들고 있으면서 계산한다. (친구 목록 API 쿼리 수를 늘리지 않기 위함)
 * 이미지는 앱이 견종(puppyType)으로 직접 보여주므로 서버에서 계산하지 않는다.
 *
 * puppy 도메인의 Repository는 조회만 하고 수정하지 않는다.
 * 테이블 내용을 바꿨다면 서버를 재시작하거나 {@link #refresh()}를 호출해야 반영된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PuppyProfileCache {

    private final LevelExpRepository levelExpRepository;

    // 첫 사용 시점에 로딩한다. (애플리케이션 기동 시 DB가 없어도 뜰 수 있도록 @PostConstruct는 쓰지 않는다)
    private volatile Snapshot snapshot;

    /**
     * 강아지 한 마리의 표시용 프로필(레벨)을 계산한다.
     * 강아지가 없으면(온보딩 미완료) 레벨이 null인 프로필을 돌려준다.
     */
    public PuppyProfile profileOf(Puppy puppy) {
        if (puppy == null) {
            return PuppyProfile.EMPTY;
        }
        Snapshot current = getSnapshot();
        return new PuppyProfile(current.levelOf(puppy.getPuppyExp()));
    }

    // 캐시를 비우고 다음 사용 시 다시 로딩한다.
    public void refresh() {
        this.snapshot = null;
    }

    private Snapshot getSnapshot() {
        Snapshot current = snapshot;
        if (current == null) {
            synchronized (this) {
                current = snapshot;
                if (current == null) {
                    current = load();
                    snapshot = current;
                }
            }
        }
        return current;
    }

    private Snapshot load() {
        List<LevelExp> levels = levelExpRepository.findAll();
        log.info("[PUPPY PROFILE CACHE] 로딩 완료: levelExp={}건", levels.size());
        return new Snapshot(levels);
    }

    /** 표시용 프로필. level이 null이면 강아지가 없거나 레벨 정보를 찾지 못한 것이다. */
    public record PuppyProfile(Integer level) {
        static final PuppyProfile EMPTY = new PuppyProfile(null);
    }

    // 로딩 시점의 불변 스냅샷
    private static final class Snapshot {
        private final List<LevelExp> levelsByMinExp;

        Snapshot(List<LevelExp> levels) {
            this.levelsByMinExp = levels.stream()
                    .sorted(Comparator.comparingInt(LevelExp::getMinExp))
                    .toList();
        }

        // LevelExpRepository.findByExp와 같은 구간 규칙(min <= exp < max)으로 레벨을 찾는다.
        // 경험치가 마지막 구간을 넘어서면 최고 레벨로, 첫 구간보다 작으면 null로 본다.
        Integer levelOf(Integer exp) {
            if (exp == null || levelsByMinExp.isEmpty()) {
                return null;
            }
            for (LevelExp levelExp : levelsByMinExp) {
                if (levelExp.getMinExp() <= exp && exp < levelExp.getMaxExp()) {
                    return levelExp.getLevel();
                }
            }
            LevelExp top = levelsByMinExp.get(levelsByMinExp.size() - 1);
            return exp >= top.getMaxExp() ? top.getLevel() : null;
        }
    }
}
