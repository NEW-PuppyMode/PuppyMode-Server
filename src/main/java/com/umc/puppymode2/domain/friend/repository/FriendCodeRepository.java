package com.umc.puppymode2.domain.friend.repository;

import com.umc.puppymode2.domain.friend.entity.FriendCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FriendCodeRepository extends JpaRepository<FriendCode, Long> {

    // 내 코드 조회 (없으면 최초 조회 시 발급)
    Optional<FriendCode> findByUserId(Long userId);

    // 친구 요청 시 상대가 입력한 코드로 코드 주인을 찾는다.
    Optional<FriendCode> findByCode(String code);
}
