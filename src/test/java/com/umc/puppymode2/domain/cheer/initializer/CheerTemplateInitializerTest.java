package com.umc.puppymode2.domain.cheer.initializer;

import com.umc.puppymode2.domain.cheer.entity.CheerTemplate;
import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import com.umc.puppymode2.domain.cheer.repository.CheerTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheerTemplateInitializerTest {

    @Mock
    private CheerTemplateRepository cheerTemplateRepository;

    @InjectMocks
    private CheerTemplateInitializer initializer;

    @Test
    void 문구_테이블이_비어_있으면_기본_문구를_등록한다() {
        when(cheerTemplateRepository.count()).thenReturn(0L);

        initializer.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CheerTemplate>> captor = ArgumentCaptor.forClass(List.class);
        verify(cheerTemplateRepository).saveAll(captor.capture());
        List<CheerTemplate> seeds = captor.getValue();
        // 확정된 문구: 장난 2 + 위로 5 + 응원 5 (장난 3개는 기획 확정 전)
        assertEquals(12, seeds.size());
        assertEquals(2, countOf(seeds, CheerCategory.PRANK));
        assertEquals(5, countOf(seeds, CheerCategory.COMFORT));
        assertEquals(5, countOf(seeds, CheerCategory.CHEER));
    }

    @Test
    void 분류_안에서_display_order는_1부터_순서대로_부여된다() {
        when(cheerTemplateRepository.count()).thenReturn(0L);

        initializer.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CheerTemplate>> captor = ArgumentCaptor.forClass(List.class);
        verify(cheerTemplateRepository).saveAll(captor.capture());
        List<CheerTemplate> comfort = captor.getValue().stream()
                .filter(t -> t.getCategory() == CheerCategory.COMFORT).toList();
        assertEquals(List.of(1, 2, 3, 4, 5), comfort.stream().map(CheerTemplate::getDisplayOrder).toList());
        assertEquals("그래 마실 수도 있지", comfort.get(0).getMessage());
    }

    @Test
    void 이미_문구가_있으면_다시_넣지_않는다() {
        // 운영 중 DB에서 문구를 수정/비활성화해도 재시작할 때 원래 문구가 되살아나면 안 된다.
        when(cheerTemplateRepository.count()).thenReturn(3L);

        initializer.run(null);

        verify(cheerTemplateRepository, never()).saveAll(anyList());
    }

    @Test
    void 여러_인스턴스가_동시에_넣다가_충돌해도_기동을_막지_않는다() {
        when(cheerTemplateRepository.count()).thenReturn(0L);
        when(cheerTemplateRepository.saveAll(anyList())).thenThrow(new DataIntegrityViolationException("uk_cheer_template_message"));

        assertDoesNotThrow(() -> initializer.run(null));
    }

    @Test
    void 조회_중_장애가_나도_기동을_막지_않는다() {
        when(cheerTemplateRepository.count()).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> initializer.run(null));
    }

    private long countOf(List<CheerTemplate> seeds, CheerCategory category) {
        return seeds.stream().filter(t -> t.getCategory() == category).count();
    }
}
