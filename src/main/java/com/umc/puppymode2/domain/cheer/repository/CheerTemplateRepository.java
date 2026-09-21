package com.umc.puppymode2.domain.cheer.repository;

import com.umc.puppymode2.domain.cheer.entity.CheerTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheerTemplateRepository extends JpaRepository<CheerTemplate, Long> {

    // 활성 문구 전체. 분류별로 묶는 것은 호출하는 쪽에서 하므로, 분류 안의 순서(display_order)만 보장한다.
    List<CheerTemplate> findAllByActiveTrueOrderByDisplayOrderAscCheerTemplateIdAsc();

    // 응원 보내기 시 문구 검증: 존재하고 활성인 문구만 사용할 수 있다.
    Optional<CheerTemplate> findByCheerTemplateIdAndActiveTrue(Long cheerTemplateId);
}
