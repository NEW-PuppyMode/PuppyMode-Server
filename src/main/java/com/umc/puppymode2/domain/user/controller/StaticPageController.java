package com.umc.puppymode2.domain.user.controller;

import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StaticPageController {

    // TODO(#213): 임시 대시보드용. Amplitude 연동 후 제거
    private final UserRepository userRepository;

    @GetMapping("/account-deletion")
    public String accountDeletionPage() {
        return "account-deletion";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicyPage() { return "privacy-policy"; }

    // TODO(#213): 임시 대시보드 페이지. Amplitude 연동 후 제거
    // 임시 페이지라 서비스 계층 없이 Repository 직접 호출
    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        model.addAttribute("userCount", userRepository.countByStatus(UserStatus.NORMAL));
        model.addAttribute("dailySignups", userRepository.countDailySignups());
        return "dashboard";
    }
}