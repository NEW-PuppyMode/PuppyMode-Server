package com.umc.puppymode2.domain.cheer.service;

import com.umc.puppymode2.domain.cheer.converter.CheerConverter;
import com.umc.puppymode2.domain.cheer.dto.ReceivedCheerListResponseDTO;
import com.umc.puppymode2.domain.cheer.entity.Cheer;
import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.util.TimeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheerQueryServiceImpl implements CheerQueryService {

    private final CheerRepository cheerRepository;
    private final UserRepository userRepository;
    private final PuppyRepository puppyRepository;
    private final PuppyProfileCache puppyProfileCache;
    private final CheerTimePolicy timePolicy;
    private final CheerConverter converter;

    @Override
    public ReceivedCheerListResponseDTO getReceivedCheers(Long myUserId) {
        // 만료 전이고, 보낸 사람이 지금도 나와 친구이며 NORMAL인 응원만 조회된다. (조건은 Repository 쿼리에 있다)
        // 이 조회는 친구의 음주 기록(DrinkHistory)을 보지 않는다. 응원을 받은 뒤 친구가 기록을 수정해도 목록은 그대로다.
        List<Cheer> cheers = cheerRepository.findReceivable(myUserId, LocalDateTime.now(), UserStatus.NORMAL);
        if (cheers.isEmpty()) {
            return converter.toReceivedListDto(List.of());
        }

        List<Long> senderIds = cheers.stream().map(Cheer::getSenderId).distinct().toList();
        Map<Long, User> senders = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        Map<Long, Puppy> puppies = puppyRepository.findAllByUserUserIdIn(new ArrayList<>(senderIds)).stream()
                .collect(Collectors.toMap(p -> p.getUser().getUserId(), Function.identity(), (a, b) -> a));

        LocalDate todayKst = LocalDate.now(TimeConstants.KST);

        List<ReceivedCheerListResponseDTO.Item> items = new ArrayList<>();
        for (Cheer cheer : cheers) {
            User sender = senders.get(cheer.getSenderId());
            if (sender == null) {
                continue;
            }
            items.add(converter.toReceivedItem(
                    cheer,
                    sender,
                    puppyProfileCache.profileOf(puppies.get(sender.getUserId())),
                    timePolicy.calculateExpiresIn(cheer.getExpiresAt(), todayKst)));
        }
        return converter.toReceivedListDto(items);
    }
}
