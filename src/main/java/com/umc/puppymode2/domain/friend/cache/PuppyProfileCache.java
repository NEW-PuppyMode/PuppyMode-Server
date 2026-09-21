package com.umc.puppymode2.domain.friend.cache;

import com.umc.puppymode2.domain.puppy.entity.LevelExp;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.entity.PuppyAppearance;
import com.umc.puppymode2.domain.puppy.entity.PuppyType;
import com.umc.puppymode2.domain.puppy.repository.LevelExpRepository;
import com.umc.puppymode2.domain.puppy.repository.PuppyAppearanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 친구의 레벨·프로필 이미지를 계산하기 위한 메모리 캐시.
 *
 * 레벨은 Puppy에 저장된 값이 아니라 puppy_exp를 LevelExp 구간에 대입해서 구하고,
 * 이미지는 (puppy_type, 레벨)을 PuppyAppearance 구간에 대입해서 구한다.
 * 이 두 테이블은 거의 바뀌지 않는 작은 고정 테이블이라, 친구 N명마다 조회하거나 조인하지 않고
 * 한 번만 읽어 메모리에 들고 있으면서 계산한다. (친구 목록 API 쿼리 수를 늘리지 않기 위함)
 *
 * puppy 도메인의 Repository는 조회만 하고 수정하지 않는다.
 * 테이블 내용을 바꿨다면 서버를 재시작하거나 {@link #refresh()}를 호출해야 반영된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PuppyProfileCache {

    private final LevelExpRepository levelExpRepository;
    private final PuppyAppearanceRepository puppyAppearanceRepository;

    // 첫 사용 시점에 로딩한다. (애플리케이션 기동 시 DB가 없어도 뜰 수 있도록 @PostConstruct는 쓰지 않는다)
    private volatile Snapshot snapshot;

    /**
     * 강아지 한 마리의 표시용 프로필(레벨, 이미지)을 계산한다.
     * 강아지가 없으면(온보딩 미완료) 전부 null인 프로필을 돌려준다.
     */
    public PuppyProfile profileOf(Puppy puppy) {
        if (puppy == null) {
            return PuppyProfile.EMPTY;
        }
        Snapshot current = getSnapshot();
        Integer level = current.levelOf(puppy.getPuppyExp());
        String imageUrl = level == null ? null : current.imageUrlOf(puppy.getPuppyType(), level);
        // 외형 정보가 없으면 강아지 종류의 대표 이미지로 대체한다. (친구 목록 전체가 실패하지 않도록)
        if (imageUrl == null && puppy.getPuppyType() != null) {
            imageUrl = puppy.getPuppyType().getImageUrl();
        }
        return new PuppyProfile(level, imageUrl);
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
        List<PuppyAppearance> appearances = puppyAppearanceRepository.findAll();
        log.info("[PUPPY PROFILE CACHE] 로딩 완료: levelExp={}건, appearance={}건", levels.size(), appearances.size());
        return new Snapshot(levels, appearances);
    }

    /** 표시용 프로필. level이 null이면 강아지가 없거나 레벨 정보를 찾지 못한 것이다. */
    public record PuppyProfile(Integer level, String imageUrl) {
        static final PuppyProfile EMPTY = new PuppyProfile(null, null);
    }

    // 로딩 시점의 불변 스냅샷
    private static final class Snapshot {
        private final List<LevelExp> levelsByMinExp;
        private final Map<PuppyType, List<PuppyAppearance>> appearancesByType = new EnumMap<>(PuppyType.class);

        Snapshot(List<LevelExp> levels, List<PuppyAppearance> appearances) {
            this.levelsByMinExp = levels.stream()
                    .sorted(Comparator.comparingInt(LevelExp::getMinExp))
                    .toList();
            for (PuppyAppearance appearance : appearances) {
                appearancesByType.computeIfAbsent(appearance.getPuppyType(), type -> new java.util.ArrayList<>())
                        .add(appearance);
            }
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

        // PuppyAppearanceRepository.findByPuppyTypeAndLevel과 같은 규칙(levelStart <= level <= levelEnd)
        String imageUrlOf(PuppyType type, int level) {
            if (type == null) {
                return null;
            }
            return appearancesByType.getOrDefault(type, List.of()).stream()
                    .filter(a -> a.getLevelStart() <= level && level <= a.getLevelEnd())
                    .map(PuppyAppearance::getImageUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst()
                    .orElse(null);
        }
    }
}
