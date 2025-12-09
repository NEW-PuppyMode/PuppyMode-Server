package com.umc.puppymode2.domain.user.auth.service;

import com.umc.puppymode2.domain.user.auth.dto.ApplePublicKeysDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * Apple Identity Token 검증을 위한 공개키 유틸리티
 */
@Slf4j
@Component
public class ApplePublicKeyUtil {

    /**
     * Apple의 공개키 정보를 사용하여 PublicKey 객체를 생성합니다.
     *
     * @param key Apple 공개키 정보
     * @return PublicKey 객체
     */
    public PublicKey generatePublicKey(ApplePublicKeysDTO.Key key) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(key.getN());
            byte[] eBytes = Base64.getUrlDecoder().decode(key.getE());

            BigInteger n = new BigInteger(1, nBytes);
            BigInteger e = new BigInteger(1, eBytes);

            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePublic(publicKeySpec);

        } catch (Exception ex) {
            log.error("[Apple Public Key] 공개키 생성 실패", ex);
            throw new RuntimeException("Failed to generate Apple public key", ex);
        }
    }
}
