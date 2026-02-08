package com.apiece.springboot_sns_sample.api.media;

import com.apiece.springboot_sns_sample.domain.media.Media;
import com.apiece.springboot_sns_sample.domain.media.PresignedUrl;
import com.apiece.springboot_sns_sample.domain.media.MediaStatus;
import com.apiece.springboot_sns_sample.domain.media.MediaType;
import com.apiece.springboot_sns_sample.domain.media.PresignedUrlPart;

import java.time.LocalDateTime;
import java.util.List;

public record MediaInitResponse(
        Long id,
        MediaType mediaType,
        String path,
        MediaStatus status,
        Long userId,
        String presignedUrl,
        String uploadId,
        List<PresignedUrlPart> presignedUrlParts,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static MediaInitResponse from(PresignedUrl presignedUrl) {
        Media media = presignedUrl.media();

        return new MediaInitResponse(
                media.getId(),
                media.getMediaType(),
                media.getPath(),
                media.getStatus(),
                media.getUserId(),
                presignedUrl.presignedUrl(),
                presignedUrl.uploadId(),
                presignedUrl.presignedUrlParts(),
                media.getCreatedAt(),
                media.getModifiedAt()
        );
    }
}
