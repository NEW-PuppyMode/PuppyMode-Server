package com.umc.puppymode2.domain.cheer.service;

import com.umc.puppymode2.domain.cheer.converter.CheerConverter;
import com.umc.puppymode2.domain.cheer.dto.CheerSendRequestDTO;
import com.umc.puppymode2.domain.cheer.dto.CheerSendResponseDTO;
import com.umc.puppymode2.domain.cheer.entity.Cheer;
import com.umc.puppymode2.domain.cheer.entity.CheerTemplate;
import com.umc.puppymode2.domain.cheer.event.CheerReceivedEvent;
import com.umc.puppymode2.domain.cheer.exception.CheerErrorStatus;
import com.umc.puppymode2.domain.cheer.repository.CheerFriendshipRepository;
import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import com.umc.puppymode2.domain.cheer.repository.CheerTemplateRepository;
import com.umc.puppymode2.domain.friend.repository.FriendDrinkRecordProjection;
import com.umc.puppymode2.domain.friend.repository.FriendDrinkRecordRepository;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.util.TimeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheerCommandServiceImpl implements CheerCommandService {

    private final CheerRepository cheerRepository;
    private final CheerTemplateRepository cheerTemplateRepository;
    private final CheerFriendshipRepository cheerFriendshipRepository;
    private final FriendDrinkRecordRepository friendDrinkRecordRepository;
    private final UserRepository userRepository;
    private final CheerTimePolicy timePolicy;
    private final CheerConverter converter;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public CheerSendResponseDTO sendCheer(Long myUserId, Long friendUserId, CheerSendRequestDTO request) {

        ZonedDateTime nowKst = ZonedDateTime.now(TimeConstants.KST);
        LocalDate todayKst = nowKst.toLocalDate();
        LocalDate targetDate = request.getTargetDate();

        // 1. 나와 상대가 친구이고 상대가 NORMAL 이어야 한다.
        //    친구가 아닌 경우와 상대가 비활성인 경우를 같은 응답으로 처리해, 친구가 아닌 사람의 상태를 알려주지 않는다.
        //    (내가 나에게 보내는 경우도 친구 관계가 없으므로 여기서 걸린다)
        //    친구 관계 행은 공유 잠금으로 조회한다. 이 트랜잭션이 끝날 때까지 친구 삭제가 기다리므로,
        //    확인한 뒤 응원을 저장하기 전에 친구가 삭제되어 응원이 남는 경쟁 상태를 막는다. (CheerFriendshipRepository 참고)
        boolean isFriend = cheerFriendshipRepository.findWithSharedLock(
                Math.min(myUserId, friendUserId), Math.max(myUserId, friendUserId)).isPresent();
        if (!isFriend) {
            throw new GeneralException(CheerErrorStatus.NOT_FRIENDS);
        }
        userRepository.findById(friendUserId)
                .filter(user -> user.getStatus() == UserStatus.NORMAL)
                .orElseThrow(() -> new GeneralException(CheerErrorStatus.NOT_FRIENDS));

        // 차단하면 친구 관계가 삭제되므로 차단 관계는 위의 친구 확인에서 NOT_FRIENDS로 걸러진다. (별도 검사 불필요)

        // 2. 문구가 존재하고 활성이어야 한다.
        CheerTemplate template = cheerTemplateRepository.findByCheerTemplateIdAndActiveTrue(request.getTemplateId())
                .orElseThrow(() -> new GeneralException(CheerErrorStatus.TEMPLATE_NOT_FOUND));

        // 3. 응원 대상 날짜는 어제 또는 오늘(KST)이고, 그 날짜에 친구가 실제로 마셨어야 한다.
        //    클라이언트가 보낸 targetDate를 믿지 않고 서버가 다시 검증한다.
        if (!timePolicy.isCheerableDate(todayKst, targetDate) || !friendDrankOn(friendUserId, targetDate)) {
            throw new GeneralException(CheerErrorStatus.INVALID_TARGET_DATE);
        }

        // 4. 같은 날짜 건을 이미 보냈다면 409. 먼저 확인해 불필요한 INSERT 실패를 피하고,
        //    동시 요청은 아래 UNIQUE 위반 처리가 최종적으로 막는다.
        if (cheerRepository.existsBySenderIdAndReceiverIdAndTargetDate(myUserId, friendUserId, targetDate)) {
            throw new GeneralException(CheerErrorStatus.CHEER_ALREADY_SENT);
        }

        LocalDateTime expiresAt = timePolicy.calculateExpiresAt(nowKst);
        try {
            // 문구와 분류는 스냅샷으로 복사해 저장한다. (템플릿이 나중에 바뀌어도 받은 응원 표시는 그대로)
            Cheer saved = cheerRepository.saveAndFlush(Cheer.of(myUserId, friendUserId, template, targetDate, expiresAt));
            eventPublisher.publishEvent(new CheerReceivedEvent(friendUserId, myUserId, template.getMessage()));

            // 응원을 보낸 뒤 친구가 기록을 수정해(is_drink true -> false) 이 날짜에 음주 기록이 없어져도
            // 이미 보낸 응원은 그대로 유지한다. (여기서는 저장만 하고, 이후 DrinkHistory 변경을 따라가지 않는다)
            return converter.toSendDto(saved);
        } catch (DataIntegrityViolationException e) {
            // 더블탭·동시 요청으로 UNIQUE(sender_id, receiver_id, target_date)에 걸린 경우
            throw new GeneralException(CheerErrorStatus.CHEER_ALREADY_SENT);
        }
    }

    @Transactional
    @Override
    public void markAllReceivedAsRead(Long myUserId) {
        // 읽음 시각도 다른 시각 컬럼과 같은 기준(JVM 기본 타임존)으로 기록한다.
        cheerRepository.markAllAsRead(myUserId, LocalDateTime.now(), UserStatus.NORMAL);
    }

    // 친구가 해당 날짜에 실제로 마셨는지. is_drink = false 인 행은 마신 것이 아니며,
    // DrinkHistory에 (user_id, drink_date) 유니크가 없어 같은 날 행이 여러 개일 수 있으므로 하나라도 true면 마신 것으로 본다.
    private boolean friendDrankOn(Long friendUserId, LocalDate date) {
        List<FriendDrinkRecordProjection> records =
                friendDrinkRecordRepository.findRecords(List.of(friendUserId), List.of(date));
        return records.stream().anyMatch(record -> Boolean.TRUE.equals(record.getIsDrink()));
    }
}
