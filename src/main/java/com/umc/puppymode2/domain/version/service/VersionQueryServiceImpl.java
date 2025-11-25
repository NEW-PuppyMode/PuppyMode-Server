package com.umc.puppymode2.domain.version.service;

import com.umc.puppymode2.domain.version.dto.VersionResponseDto;
import com.umc.puppymode2.domain.version.entity.AppVersion;
import com.umc.puppymode2.domain.version.repository.AppVersionRepository;
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
    public VersionResponseDto getVersionInfo(String osType) {
        AppVersion appVersion = appVersionRepository.findByOsType(osType)
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 OS 타입입니다."));

        return VersionResponseDto.builder()
                .latestVersion(appVersion.getLatestVersion())
                .updateUrl(appVersion.getUpdateUrl())
                .build();
    }
}