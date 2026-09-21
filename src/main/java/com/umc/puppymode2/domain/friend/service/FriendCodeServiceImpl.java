package com.umc.puppymode2.domain.friend.service;

import com.umc.puppymode2.domain.friend.converter.FriendConverter;
import com.umc.puppymode2.domain.friend.dto.FriendCodeResponseDTO;
import com.umc.puppymode2.domain.friend.entity.FriendCode;
import com.umc.puppymode2.domain.friend.repository.FriendCodeRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendCodeServiceImpl implements FriendCodeService {

    // 4자리에서 시작해, 그 자릿수의 코드가 좀처럼 발급되지 않을 만큼 포화되면 한 자리씩 늘린다.
    private static final int INITIAL_LENGTH = 4;
    private static final int MAX_LENGTH = 8;
    private static final int ATTEMPTS_PER_LENGTH = 20;

    private final FriendCodeRepository friendCodeRepository;
    private final FriendConverter converter;

    // 예측하기 어려운 코드를 만들기 위해 SecureRandom 사용
    private final SecureRandom random = new SecureRandom();

    /**
     * 일부러 @Transactional을 붙이지 않았다.
     *
     * 코드 INSERT가 UNIQUE 위반으로 실패하면 그 트랜잭션은 롤백 대상이 되어 더는 쓸 수 없다.
     * 트랜잭션 없이 호출하면 repository 호출마다 각자 트랜잭션이 열리고 닫히므로,
     * 실패한 시도가 다음 재시도에 영향을 주지 않는다.
     */
    @Override
    public FriendCodeResponseDTO getOrIssueMyCode(Long userId) {
        Optional<FriendCode> existing = friendCodeRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return converter.toCodeDto(existing.get().getCode());
        }

        for (int length = INITIAL_LENGTH; length <= MAX_LENGTH; length++) {
            for (int attempt = 0; attempt < ATTEMPTS_PER_LENGTH; attempt++) {
                String code = generateCode(length);
                try {
                    friendCodeRepository.saveAndFlush(FriendCode.of(userId, code));
                    return converter.toCodeDto(code);
                } catch (DataIntegrityViolationException e) {
                    // UNIQUE 위반 사유는 두 가지다.
                    //  1) 같은 사용자가 동시에 최초 조회를 해서 다른 요청이 먼저 코드를 만들었다 → 그 코드를 돌려준다.
                    //  2) 다른 사용자와 코드가 겹쳤다 → 새 코드로 재시도한다.
                    Optional<FriendCode> raced = friendCodeRepository.findByUserId(userId);
                    if (raced.isPresent()) {
                        return converter.toCodeDto(raced.get().getCode());
                    }
                }
            }
            log.warn("[FRIEND CODE] {}자리 코드 발급이 {}회 연속 충돌했습니다. 자릿수를 늘립니다.", length, ATTEMPTS_PER_LENGTH);
        }

        // 8자리까지 늘려도 발급하지 못하는 경우는 사실상 없다.
        throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
    }

    // 맨 앞자리가 0이 아닌 length 자리 숫자 문자열 (예: 4자리면 1000~9999)
    private String generateCode(int length) {
        long lowerBound = (long) Math.pow(10, length - 1);
        long upperBound = (long) Math.pow(10, length);
        return String.valueOf(random.nextLong(lowerBound, upperBound));
    }
}
