package com.umc.puppymode2.domain.user.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Apple Public Keys 응답 DTO
 * Apple의 /auth/keys 엔드포인트로부터 받는 공개키 목록
 */
@Getter
@NoArgsConstructor
public class ApplePublicKeysDTO {

    @JsonProperty("keys")
    private List<Key> keys;

    @Getter
    @NoArgsConstructor
    public static class Key {
        @JsonProperty("kty")
        private String kty;

        @JsonProperty("kid")
        private String kid;

        @JsonProperty("use")
        private String use;

        @JsonProperty("alg")
        private String alg;

        @JsonProperty("n")
        private String n;

        @JsonProperty("e")
        private String e;
    }
}
