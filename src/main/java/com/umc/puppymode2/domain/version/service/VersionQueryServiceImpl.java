package com.umc.puppymode2.domain.version.service;

import com.umc.puppymode2.domain.version.dto.VersionResponseDto;
import com.umc.puppymode2.domain.version.entity.AppVersion;
import com.umc.puppymode2.domain.version.repository.AppVersionRepository;
import com.umc.puppymode2.global.apiPayload.code.status.ErrorStatus;
import com.umc.puppymode2.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionQueryServiceImpl implements VersionQueryService {

    private final AppVersionRepository appVersionRepository;

    @Override
    @Transactional(readOnly = true)
    public VersionResponseDto getVersionInfo(String osType,  String currentVersion) {
        AppVersion appVersion = appVersionRepository.findByOsType(osType)
                .orElseThrow(() -> new GeneralException(ErrorStatus.UNSUPPORTED_OS_TYPE));

        boolean updateRequired = isLowerVersion(currentVersion, appVersion.getMinVersion());
        boolean updateAvailable = isLowerVersion(currentVersion, appVersion.getLatestVersion());

        return VersionResponseDto.builder()
                .latestVersion(appVersion.getLatestVersion())
                .updateUrl(appVersion.getUpdateUrl())
                .updateRequired(updateRequired)
                .updateAvailable(updateAvailable)
                .build();
    }

    private boolean isLowerVersion(String current, String target) {
        String[] currentParts = current.split("\\.");
        String[] targetParts = target.split("\\.");

        for (int i = 0; i < Math.max(currentParts.length, targetParts.length); i++) {
            int c = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int t = i < targetParts.length ? Integer.parseInt(targetParts[i]) : 0;

            if (c < t) return true;
            if (c > t) return false;
        }

        return false;
    }
}