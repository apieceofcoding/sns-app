package com.apiece.springboot_sns_sample.domain.media;

import java.util.List;

public record PresignedUrl(
        Media media,
        String presignedUrl,
        String uploadId,
        List<PresignedUrlPart> presignedUrlParts
) {

    public static PresignedUrl forSingleUpload(Media media, String presignedUrl) {
        return new PresignedUrl(media, presignedUrl, null, null);
    }

    public static PresignedUrl forMultipartUpload(Media media, String uploadId, List<PresignedUrlPart> presignedUrlParts) {
        return new PresignedUrl(media, null, uploadId, presignedUrlParts);
    }
}
