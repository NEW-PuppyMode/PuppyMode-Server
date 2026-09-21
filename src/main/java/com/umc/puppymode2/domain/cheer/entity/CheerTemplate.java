package com.umc.puppymode2.domain.cheer.entity;

import com.umc.puppymode2.domain.cheer.entity.enums.CheerCategory;
import com.umc.puppymode2.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 응원 문구 템플릿 (시드 데이터).
 *
 * 문구를 코드가 아니라 테이블로 관리하므로, 배포 없이 DB에서 문구를 추가/수정/비활성화할 수 있다.
 * 이미 보낸 응원에는 발송 시점의 문구가 Cheer.messageSnapshot으로 복사되어 있어서,
 * 템플릿 문구를 나중에 바꿔도 받은 응원의 표시는 변하지 않는다.
 */
@Entity
@Table(
        name = "cheer_template",
        uniqueConstraints = {
                // 서버 시작 시 시드 데이터를 넣다가 여러 인스턴스가 동시에 실행돼도 같은 문구가 중복 저장되지 않게 한다.
                @UniqueConstraint(name = "uk_cheer_template_message", columnNames = {"category", "message"})
        },
        indexes = {
                @Index(name = "idx_cheer_template_cat", columnList = "category, is_active, display_order")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheerTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cheer_template_id")
    private Long cheerTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    private CheerCategory category;

    @Column(name = "message", nullable = false, length = 100)
    private String message;

    // 분류 안에서의 노출 순서 (작을수록 먼저)
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // false이면 새 응원에는 쓸 수 없고 문구 목록에도 나오지 않는다. (이미 보낸 응원은 영향 없음)
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public static CheerTemplate of(CheerCategory category, String message, int displayOrder) {
        CheerTemplate template = new CheerTemplate();
        template.category = category;
        template.message = message;
        template.displayOrder = displayOrder;
        template.active = true;
        return template;
    }
}
