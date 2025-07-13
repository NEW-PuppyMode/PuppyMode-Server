package com.umc.puppymode2.global.auth.context;

import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import com.umc.puppymode2.global.security.UserAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityUserContext implements UserContext {

    /**
     * 현재 인증된 User의 Id를 가져옵니다.
     */
    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.warn("[SecurityUserContext] 인증 객체가 null입니다.");
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        if (!(authentication instanceof UserAuthentication)) {
            log.warn("[SecurityUserContext] 인증 객체 타입이 예상과 다릅니다. 현재 타입: {}",
                    authentication.getClass().getName());
            log.debug("[SecurityUserContext] authentication 전체 정보: {}", authentication);
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        UserAuthentication userAuthentication = (UserAuthentication) authentication;
        Long userId = userAuthentication.getUserId();

        if (userId == null) {
            log.warn("[SecurityUserContext] 인증된 사용자 ID가 null입니다.");
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        return userId;
    }
}