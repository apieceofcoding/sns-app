package com.apiece.springboot_sns_sample.api.media;

import com.apiece.springboot_sns_sample.domain.media.MediaType;

import java.util.Map;

public record MediaCreateRequest(
        MediaType mediaType,
        String path,
        Long postId,
        Map<String, Object> attributes
) {
}
