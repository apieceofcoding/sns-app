package com.apiece.springboot_sns_sample.api.media;

import com.apiece.springboot_sns_sample.domain.media.MultipartUploaded;

import java.util.List;

public record MediaUploadedRequest(
        Long mediaId,
        List<MultipartUploaded> parts
) {
}
