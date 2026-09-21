package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.cheer.repository.CheerRepository;
import com.umc.puppymode2.domain.cheer.repository.CheerSentProjection;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache;
import com.umc.puppymode2.domain.friend.cache.PuppyProfileCache.PuppyProfile;
import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.friend.dto.FriendListResponseDTO;
import com.umc.puppymode2.domain.friend.dto.FriendProfileResponseDTO;
import com.umc.puppymode2.domain.friend.dto.ReceivedFriendRequestListResponseDTO;
import com.umc.puppymode2.domain.friend.entity.FriendRequest;
import com.umc.puppymode2.domain.friend.entity.enums.FriendRequestStatus;
import com.umc.puppymode2.domain.friend.exception.FriendErrorStatus;
import com.umc.puppymode2.domain.friend.repository.FriendDrinkRecordProjection;
import com.umc.puppymode2.domain.friend.repository.FriendDrinkRecordRepository;
import com.umc.puppymode2.domain.friend.repository.FriendRequestRepository;
import com.umc.puppymode2.domain.friend.repository.FriendshipRepository;
import com.umc.puppymode2.domain.friend.service.FriendStatusCalculator.FriendStatus;
import com.umc.puppymode2.domain.puppy.entity.Puppy;
import com.umc.puppymode2.domain.puppy.repository.PuppyRepository;
import com.umc.puppymode2.domain.user.entity.User;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.util.TimeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendQueryServiceImpl implements FriendQueryService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendDrinkRecordRepository friendDrinkRecordRepository;
    private final CheerRepository cheerRepository;
    private final UserRepository userRepository;
    private final PuppyRepository puppyRepository;
    private final PuppyProfileCache puppyProfileCache;
    private final FriendStatusCalculator statusCalculator;
    private final FriendConverter converter;

    @Override
    public ReceivedFriendRequestListResponseDTO getReceivedRequests(Long myUserId) {
        List<FriendRequest> pending =
                friendRequestRepository.findAllByReceiverAndStatus(myUserId, FriendRequestStatus.PENDING);
        if (pending.isEmpty()) {
            return converter.toReceivedDto(List.of());
        }

        // 서로 동시에 요청해서 이미 친구가 됐는데 PENDING이 남은 경우를 목록에서 걸러낸다.
        Set<Long> friendIds = new HashSet<>(friendshipRepository.findFriendIds(myUserId));

        List<Long> requesterIds = pending.stream()
                .map(FriendRequest::getRequesterId)
                .filter(id -> !friendIds.contains(id))
                .distinct()
                .toList();

        // 요청자가 NORMAL이 아니면(탈퇴/휴면) 목록에서 제외한다.
        Map<Long, User> users = findNormalUsers(requesterIds);
        Map<Long, Puppy> puppies = findPuppies(users.keySet());

        // TODO: 소셜 3/4(차단)에서 차단 관계인 요청자도 이 목록에서 제외한다.

        List<ReceivedFriendRequestListResponseDTO.Item> items = new ArrayList<>();
        for (FriendRequest request : pending) {
            User requester = users.get(request.getRequesterId());
            if (requester == null) {
                continue;
            }
            Puppy puppy = puppies.get(requester.getUserId());
            items.add(converter.toReceivedItem(request, requester, puppy, puppyProfileCache.profileOf(puppy)));
        }
        return converter.toReceivedDto(items);
    }

    @Override
    public FriendListResponseDTO getFriends(Long myUserId) {
        List<Long> friendIds = friendshipRepository.findFriendIds(myUserId);
        if (friendIds.isEmpty()) {
            return converter.toFriendListDto(List.of());
        }

        // 쿼리 수를 친구 수와 무관하게 고정하기 위해 사용자/강아지/음주 기록을 각각 한 번에 IN으로 조회한다.
        Map<Long, User> users = findNormalUsers(friendIds);
        Map<Long, Puppy> puppies = findPuppies(users.keySet());

        LocalDate today = LocalDate.now(TimeConstants.KST);
        Map<Long, Map<LocalDate, Boolean>> drinkByFriend = findDrinkRecords(users.keySet(), today);

        // 내가 각 친구에게 어제/오늘 날짜로 이미 보낸 응원. cheer.state의 SENT 판정과 응원 대상 날짜 계산에 쓰인다.
        Map<Long, Set<LocalDate>> sentByFriend = findSentCheerDates(myUserId, users.keySet(), today);

        // 가나다순 정렬. DB collation에 의존하지 않고 한국어 Collator로 정렬한다.
        Collator collator = Collator.getInstance(Locale.KOREAN);
        List<User> sortedFriends = users.values().stream()
                .sorted(Comparator.comparing(User::getUsername, Comparator.nullsLast(collator::compare)))
                .toList();

        List<FriendListResponseDTO.Item> items = new ArrayList<>();
        for (User friend : sortedFriends) {
            Puppy puppy = puppies.get(friend.getUserId());
            FriendStatus status = statusCalculator.calculate(
                    today,
                    drinkByFriend.getOrDefault(friend.getUserId(), Map.of()),
                    sentByFriend.getOrDefault(friend.getUserId(), Set.of()));
            items.add(converter.toFriendItem(friend, puppy, puppyProfileCache.profileOf(puppy), status));
        }
        return converter.toFriendListDto(items);
    }

    @Override
    public FriendProfileResponseDTO getFriendProfile(Long myUserId, Long friendUserId) {
        // 친구가 아닌 사람의 존재 여부를 알려주지 않도록, 친구 관계부터 확인한다.
        boolean isFriend = friendshipRepository.existsByUserLowIdAndUserHighId(
                Math.min(myUserId, friendUserId), Math.max(myUserId, friendUserId));
        if (!isFriend) {
            throw new GeneralException(FriendErrorStatus.NOT_FRIENDS);
        }

        User friend = userRepository.findById(friendUserId)
                .filter(user -> user.getStatus() == UserStatus.NORMAL)
                .orElseThrow(() -> new GeneralException(FriendErrorStatus.FRIEND_USER_NOT_FOUND));

        Puppy puppy = puppyRepository.findByUser_UserId(friendUserId).orElse(null);
        return converter.toProfileDto(friend, puppy, puppyProfileCache.profileOf(puppy));
    }

    // ID 목록에서 상태가 NORMAL인 사용자만 userId -> User 맵으로 반환한다.
    private Map<Long, User> findNormalUsers(Collection<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .filter(user -> user.getStatus() == UserStatus.NORMAL)
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
    }

    // userId -> Puppy 맵. 온보딩을 마치지 않아 강아지가 없는 사용자는 맵에 없다.
    private Map<Long, Puppy> findPuppies(Collection<Long> userIds) {
        return puppyRepository.findAllByUserUserIdIn(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(p -> p.getUser().getUserId(), Function.identity(), (a, b) -> a));
    }

    // 내가 친구들에게 어제/오늘 날짜로 보낸 응원을 친구 userId -> 응원 대상 날짜 집합 으로 모은다.
    private Map<Long, Set<LocalDate>> findSentCheerDates(Long myUserId, Collection<Long> friendIds, LocalDate today) {
        List<CheerSentProjection> sent =
                cheerRepository.findSentTargets(myUserId, friendIds, List.of(today.minusDays(1), today));

        Map<Long, Set<LocalDate>> result = new HashMap<>();
        for (CheerSentProjection cheer : sent) {
            result.computeIfAbsent(cheer.getReceiverId(), id -> new HashSet<>()).add(cheer.getTargetDate());
        }
        return result;
    }

    // 친구들의 어제/오늘 음주 기록을 userId -> (날짜 -> 마셨는지) 로 모은다.
    // 같은 날짜에 행이 여러 개여도 하나라도 is_drink = true 면 마신 것으로 합친다.
    private Map<Long, Map<LocalDate, Boolean>> findDrinkRecords(Collection<Long> friendIds, LocalDate today) {
        List<FriendDrinkRecordProjection> records =
                friendDrinkRecordRepository.findRecords(friendIds, List.of(today.minusDays(1), today));

        Map<Long, Map<LocalDate, Boolean>> result = new HashMap<>();
        for (FriendDrinkRecordProjection record : records) {
            result.computeIfAbsent(record.getUserId(), id -> new HashMap<>())
                    .merge(record.getDrinkDate(), Boolean.TRUE.equals(record.getIsDrink()), Boolean::logicalOr);
        }
        return result;
    }
}
