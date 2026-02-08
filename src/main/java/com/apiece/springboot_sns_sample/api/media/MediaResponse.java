package com.apiece.springboot_sns_sample.api.media;

import com.apiece.springboot_sns_sample.domain.media.Media;
import com.apiece.springboot_sns_sample.domain.media.MediaStatus;
import com.apiece.springboot_sns_sample.domain.media.MediaType;

import java.time.LocalDateTime;
import java.util.Map;

public record MediaResponse(
        Long id,
        MediaType mediaType,
        String path,
        MediaStatus status,
        Long userId,
        Long fileSize,
        Map<String, Object> attributes,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static MediaResponse from(Media media) {
        return new MediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getPath(),
                media.getStatus(),
                media.getUserId(),
                media.getFileSize(),
                media.getAttributes(),
                media.getCreatedAt(),
                media.getModifiedAt()
        );
    }
}
