package com.umc.puppymode2.domain.version.service;

import com.umc.puppymode2.domain.version.dto.VersionResponseDto;

public interface VersionQueryService {
    VersionResponseDto getVersionInfo(String osType, String currentVersion);
}