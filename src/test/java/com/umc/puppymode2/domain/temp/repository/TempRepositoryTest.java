package com.umc.puppymode2.domain.temp.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import com.umc.puppymode2.domain.temp.entity.Temp;
import com.umc.puppymode2.domain.temp.entity.enums.TempStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TempRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TempRepository tempRepository;

    @Test
    @DisplayName("Temp 저장 테스트")
    void saveTemp() {
        // given
        Temp temp = Temp.builder()
                .name("테스트")
                .status(TempStatus.ACTIVE)
                .build();

        // when
        Temp savedTemp = tempRepository.save(temp);

        // then
        assertThat(savedTemp.getId()).isNotNull();
        assertThat(savedTemp.getName()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("상태별 Temp 조회 테스트")
    void findByStatus() {
        // given
        Temp activeTemp = Temp.builder()
                .name("활성")
                .status(TempStatus.ACTIVE)
                .build();

        Temp inactiveTemp = Temp.builder()
                .name("비활성")
                .status(TempStatus.INACTIVE)
                .build();

        entityManager.persistAndFlush(activeTemp);
        entityManager.persistAndFlush(inactiveTemp);

        // when
        List<Temp> activeTemps = tempRepository.findByStatus(TempStatus.ACTIVE);

        // then
        assertThat(activeTemps).hasSize(1);
        assertThat(activeTemps.get(0).getStatus()).isEqualTo(TempStatus.ACTIVE);
    }
}