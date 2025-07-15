package com.umc.puppymode2.domain.puppy.repository;

import com.umc.puppymode2.domain.puppy.entity.PuppyLevel;
import com.umc.puppymode2.domain.puppy.entity.PuppyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 관련된 엔티티/enum/repository 가 미리 생성되지 않아 임시로 구현한 파일입니다.
// 이후 다른 분들 코드도 머지되면, 해당 코드 속 파일로 import 변경하도록 하겠습니다!
public interface PuppyLevelRepository extends JpaRepository<PuppyLevel, Long> {

    Optional<PuppyLevel> findByPuppyTypeAndPuppyLevel(PuppyType puppyType, int level);
}
