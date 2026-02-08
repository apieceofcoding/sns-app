package com.apiece.springboot_sns_sample.domain.media;

import com.apiece.springboot_sns_sample.config.s3.RustFsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MultipartService {

    private static final long PART_SIZE = 8 * 1024 * 1024; // 8MB

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final RustFsProperties rustFsProperties;

    public MultipartUploadInfo initMultipartUpload(String path, String contentType, long fileSize) {
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(rustFsProperties.bucket())
                .key(path)
                .contentType(contentType)
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();

        int numberOfParts = (int) Math.ceil((double) fileSize / PART_SIZE);

        List<PresignedUrlPart> presignedUrlParts = new ArrayList<>();
        for (int partNumber = 1; partNumber <= numberOfParts; partNumber++) {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(rustFsProperties.bucket())
                    .key(path)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(rustFsProperties.presignedUrlExpirationSeconds()))
                    .uploadPartRequest(uploadPartRequest)
                    .build();

            PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            presignedUrlParts.add(new PresignedUrlPart(partNumber, presignedUrl));
        }

        return new MultipartUploadInfo(uploadId, presignedUrlParts);
    }

    public void completeMultipartUpload(String path, String uploadId, List<MultipartUploaded> parts) {
        List<CompletedPart> s3Parts = parts.stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.eTag())
                        .build())
                .toList();

        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                .parts(s3Parts)
                .build();

        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(rustFsProperties.bucket())
                .key(path)
                .uploadId(uploadId)
                .multipartUpload(completedMultipartUpload)
                .build();

        s3Client.completeMultipartUpload(completeRequest);
    }
}
