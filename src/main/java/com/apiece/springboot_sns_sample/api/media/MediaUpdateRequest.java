package com.apiece.springboot_sns_sample.api.media;

import com.apiece.springboot_sns_sample.domain.media.MediaStatus;

import java.util.Map;

public record MediaUpdateRequest(
        String path,
        MediaStatus status,
        Map<String, Object> attributes
) {
}
