package com.umc.puppymode2.domain.version.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class VersionResponseDto {
    private String latestVersion;
    private String updateUrl;
    private boolean updateRequired;
    private boolean updateAvailable;
}