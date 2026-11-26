package com.umc.puppymode2.domain.user.controller;

import com.umc.puppymode2.domain.puppy.service.PuppyLevelCalculator;
import com.umc.puppymode2.domain.user.entity.enums.UserStatus;
import com.umc.puppymode2.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        long userCount = userRepository.countByStatus(UserStatus.NORMAL);
        model.addAttribute("userCount", userCount);
        model.addAttribute("dailySignups", userRepository.countDailySignups());
        model.addAttribute("levelDistribution", buildLevelDistribution(userCount));
        return "dashboard";
    }

    // TODO(#228): 임시 대시보드용. Amplitude 연동 후 제거
    private List<LevelDistributionRow> buildLevelDistribution(long userCount) {
        Map<Integer, Long> countByLevel = new HashMap<>();
        for (UserRepository.LevelCount row : userRepository.countByLevel()) {
            countByLevel.put(row.getLevel(), row.getCount());
        }

        long maxLevelCount = 0;
        for (int level = 1; level <= PuppyLevelCalculator.MAX_LEVEL; level++) {
            maxLevelCount = Math.max(maxLevelCount, countByLevel.getOrDefault(level, 0L));
        }

        List<LevelDistributionRow> rows = new ArrayList<>();
        for (int level = 1; level <= PuppyLevelCalculator.MAX_LEVEL; level++) {
            long count = countByLevel.getOrDefault(level, 0L);
            double intensity = maxLevelCount == 0 ? 0 : (double) count / maxLevelCount;
            rows.add(new LevelDistributionRow("Lv." + level, count, ratioOf(count, userCount), intensity));
        }
        long noLevelCount = countByLevel.getOrDefault(null, 0L);
        rows.add(new LevelDistributionRow("레벨 없음", noLevelCount, ratioOf(noLevelCount, userCount), 0));
        return rows;
    }

    private double ratioOf(long count, long total) {
        return total == 0 ? 0 : count * 100.0 / total;
    }

    // TODO(#228): 임시 대시보드용. Amplitude 연동 후 제거
    public record LevelDistributionRow(String label, long count, double ratio, double intensity) {
    }
}
