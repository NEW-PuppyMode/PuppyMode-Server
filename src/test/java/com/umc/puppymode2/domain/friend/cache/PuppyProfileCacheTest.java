package com.umc.puppymode2.domain.friend.cache;

import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache.PuppyProfile;
import com.umc.puppymode2.domain.puppy.entity.LevelExp;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.entity.PuppyAppearance;
import com.umc.puppymode2.domain.puppy.entity.PuppyType;
import com.umc.puppymode2.domain.puppy.repository.LevelExpRepository;
import com.umc.puppymode2.domain.puppy.repository.PuppyAppearanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PuppyProfileCacheTest {

    @Mock
    private LevelExpRepository levelExpRepository;

    @Mock
    private PuppyAppearanceRepository puppyAppearanceRepository;

    @InjectMocks
    private PuppyProfileCache cache;

    @BeforeEach
    void setUp() {
        // 레벨 1: 0~100, 레벨 2: 100~300, 레벨 3(최고): 300~600
        lenient().when(levelExpRepository.findAll()).thenReturn(List.of(
                levelExp(3, 300, 600), levelExp(1, 0, 100), levelExp(2, 100, 300)));
        // 비숑: 레벨 1~2는 1단계, 레벨 3은 2단계 이미지
        lenient().when(puppyAppearanceRepository.findAll()).thenReturn(List.of(
                appearance(PuppyType.BICHON, 1, 2, "bichon_stage1.svg"),
                appearance(PuppyType.BICHON, 3, 3, "bichon_stage2.svg")));
    }

    @Test
    void 경험치_구간으로_레벨과_이미지를_계산한다() {
        PuppyProfile profile = cache.profileOf(puppy(PuppyType.BICHON, 150));

        assertEquals(2, profile.level());
        assertEquals("bichon_stage1.svg", profile.imageUrl());
    }

    @Test
    void 구간_경계값은_min_이상_max_미만으로_판단한다() {
        assertEquals(1, cache.profileOf(puppy(PuppyType.BICHON, 99)).level());
        assertEquals(2, cache.profileOf(puppy(PuppyType.BICHON, 100)).level());
        assertEquals(3, cache.profileOf(puppy(PuppyType.BICHON, 300)).level());
    }

    @Test
    void 경험치가_최고_레벨_구간을_넘어서면_최고_레벨로_본다() {
        PuppyProfile profile = cache.profileOf(puppy(PuppyType.BICHON, 9999));

        assertEquals(3, profile.level());
        assertEquals("bichon_stage2.svg", profile.imageUrl());
    }

    @Test
    void 외형_정보가_없으면_강아지_종류의_대표_이미지로_대체한다() {
        // 시바는 PuppyAppearance 데이터가 없다.
        PuppyProfile profile = cache.profileOf(puppy(PuppyType.SHIBA, 150));

        assertEquals(2, profile.level());
        assertEquals(PuppyType.SHIBA.getImageUrl(), profile.imageUrl());
    }

    @Test
    void 강아지가_없으면_레벨과_이미지가_null이다() {
        PuppyProfile profile = cache.profileOf(null);

        assertNull(profile.level());
        assertNull(profile.imageUrl());
    }

    @Test
    void 테이블은_처음_한_번만_읽고_이후에는_메모리에서_계산한다() {
        cache.profileOf(puppy(PuppyType.BICHON, 10));
        cache.profileOf(puppy(PuppyType.BICHON, 200));
        cache.profileOf(puppy(PuppyType.BICHON, 400));

        verify(levelExpRepository, times(1)).findAll();
        verify(puppyAppearanceRepository, times(1)).findAll();
    }

    @Test
    void refresh하면_다음_사용_때_다시_읽는다() {
        cache.profileOf(puppy(PuppyType.BICHON, 10));
        cache.refresh();
        cache.profileOf(puppy(PuppyType.BICHON, 10));

        verify(levelExpRepository, times(2)).findAll();
    }

    // LevelExp / PuppyAppearance / Puppy 는 setter·public 생성자가 없어 리플렉션으로 값을 채운다.
    private LevelExp levelExp(int level, int minExp, int maxExp) {
        LevelExp entity = instantiate(LevelExp.class);
        set(entity, "level", level);
        set(entity, "minExp", minExp);
        set(entity, "maxExp", maxExp);
        return entity;
    }

    private PuppyAppearance appearance(PuppyType type, int levelStart, int levelEnd, String imageUrl) {
        PuppyAppearance entity = instantiate(PuppyAppearance.class);
        set(entity, "puppyType", type);
        set(entity, "levelStart", levelStart);
        set(entity, "levelEnd", levelEnd);
        set(entity, "imageUrl", imageUrl);
        return entity;
    }

    private Puppy puppy(PuppyType type, int exp) {
        return Puppy.builder().puppyType(type).puppyName("test").puppyExp(exp).build();
    }

    private <T> T instantiate(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void set(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
