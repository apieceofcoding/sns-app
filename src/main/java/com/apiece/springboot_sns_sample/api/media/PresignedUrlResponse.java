package com.apiece.springboot_sns_sample.api.media;

public record PresignedUrlResponse(
        String presignedUrl,
        MediaResponse media
) {
}
