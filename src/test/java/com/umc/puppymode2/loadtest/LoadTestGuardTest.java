package com.umc.puppymode2.loadtest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadTestGuardTest {

    @Test
    void localhost와_루프백_주소만_허용한다() {
        assertTrue(LoadTestDataSetup.isLocalHost("jdbc:mysql://localhost:3306/puppymode"));
        assertTrue(LoadTestDataSetup.isLocalHost("jdbc:mysql://127.0.0.1:3306/puppymode?useSSL=false"));
        assertTrue(LoadTestDataSetup.isLocalHost("jdbc:mysql://localhost/puppymode"));
    }

    @Test
    void 원격_DB와_localhost를_흉내낸_주소는_거부한다() {
        assertFalse(LoadTestDataSetup.isLocalHost("jdbc:mysql://puppy-mode-db.example.ap-northeast-2.rds.amazonaws.com:3306/puppymode"));
        assertFalse(LoadTestDataSetup.isLocalHost("jdbc:mysql://localhost.evil.example:3306/puppymode"));
        assertFalse(LoadTestDataSetup.isLocalHost("jdbc:mysql://user@localhost@evil.example/puppymode"));
        assertFalse(LoadTestDataSetup.isLocalHost("jdbc:mysql://10.0.0.5:3306/puppymode"));
    }

    @Test
    void 호스트를_알_수_없으면_거부한다() {
        assertFalse(LoadTestDataSetup.isLocalHost(null));
        assertFalse(LoadTestDataSetup.isLocalHost(""));
        assertFalse(LoadTestDataSetup.isLocalHost("jdbc:h2:mem:testdb"));
    }
}
