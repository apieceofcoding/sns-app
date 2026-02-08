package com.apiece.springboot_sns_sample.api.media;

import com.apiece.springboot_sns_sample.domain.media.MediaType;

public record MediaInitRequest(
        MediaType mediaType,
        Long fileSize
) {
}
