package com.apiece.springboot_sns_sample.domain.media;

public record PresignedUrlPart(
        int partNumber,
        String presignedUrl
) {
}
