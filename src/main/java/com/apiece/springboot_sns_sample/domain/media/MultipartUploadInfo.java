package com.apiece.springboot_sns_sample.domain.media;

import java.util.List;

public record MultipartUploadInfo(
        String uploadId,
        List<PresignedUrlPart> presignedUrlParts
) {
}
