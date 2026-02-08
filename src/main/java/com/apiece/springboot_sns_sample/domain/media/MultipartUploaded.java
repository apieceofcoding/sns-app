package com.apiece.springboot_sns_sample.domain.media;

public record MultipartUploaded(
        int partNumber,
        String eTag
) {
}
