package com.umc.puppymode2.domain.notification.dto;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DrinkReminderMessage {

    private static final String TITLE = "오늘 음주 기록을 남겨보세요 🐶";

    private static final List<String> TEMPLATES = List.of(
            "지금 어디세요? 혼자 기다리다 심심해 죽겠어요…",
            "%s님… 절 잊으신건 아니죠?",
            "%s님… 오늘 술 드셨어요? 설마 뻗으신 건 아니죠…?",
            "술 드시고 저 잊으신 건 아니죠? 기다리고 있어요…",
            "이렇게 안오시면… 저 진짜 서운해요…"
    );

    public static String getTitle() {
        return TITLE;
    }

    public static String getBody(String username) {
        String template = TEMPLATES.get(ThreadLocalRandom.current().nextInt(TEMPLATES.size()));
        return template.contains("%s") ? String.format(template, username) : template;
    }
}