package com.apiece.springboot_sns_sample.domain.media;

import com.apiece.springboot_sns_sample.config.s3.RustFsProperties;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final long MULTIPART_THRESHOLD = 8 * 1024 * 1024; // 8MB = 8,388,608

    private final MediaRepository mediaRepository;
    private final MultipartService multipartService;
    private final S3Presigner s3Presigner;
    private final RustFsProperties rustFsProperties;

    public PresignedUrl initMedia(MediaType mediaType, Long fileSize, User user) {
        return initMedia(mediaType, fileSize, user, "posts");
    }

    public PresignedUrl initMedia(MediaType mediaType, Long fileSize, User user, String subPath) {
        String filename = UUID.randomUUID() + mediaType.fileExtension();
        String path = String.format("users/%s/%s/%s", user.getId(), subPath, filename);

        if (fileSize != null && fileSize > MULTIPART_THRESHOLD) {
            return initMultipartUpload(user, path, mediaType, fileSize);
        }

        return initSingleUpload(user, path, mediaType, fileSize);
    }

    public Media mediaUploaded(Long mediaId, List<MultipartUploaded> parts, User user) {
        Media media = getMediaById(mediaId);

        if (!media.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to update this media");
        }

        if (media.getStatus() != MediaStatus.INIT) {
            throw new IllegalArgumentException("Media is not in INIT status");
        }

        if (media.getUploadId() != null && !CollectionUtils.isEmpty(parts)) {
            multipartService.completeMultipartUpload(media.getPath(), media.getUploadId(), parts);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("parts", parts);
            media.updateAttributes(attributes);
        }

        media.updateStatus(MediaStatus.UPLOADED);

        return mediaRepository.save(media);
    }

    public Media getMediaById(Long id) {
        return mediaRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + id));
    }

    public List<Media> getMediaByUserId(Long userId) {
        return mediaRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
    }

    public void deleteMedia(Long id, User user) {
        Media media = getMediaById(id);

        if (!media.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to delete this media");
        }

        media.delete();
        mediaRepository.save(media);
    }

    public String getPresignedUrl(Long id) {
        return getPresignedUrl(getMediaById(id));
    }

    public String getPresignedUrl(Media media) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(rustFsProperties.bucket())
                .key(media.getPath())
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(rustFsProperties.presignedUrlExpirationSeconds()))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    private PresignedUrl initSingleUpload(User user, String path, MediaType mediaType, Long fileSize) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(rustFsProperties.bucket())
                .key(path)
                .contentType(mediaType.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(rustFsProperties.presignedUrlExpirationSeconds()))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedRequest.url().toString();

        Media media = Media.create(mediaType, path, user.getId(), fileSize);
        media = mediaRepository.save(media);

        return PresignedUrl.forSingleUpload(media, presignedUrl);
    }

    private PresignedUrl initMultipartUpload(User user, String path, MediaType mediaType, long fileSize) {
        MultipartUploadInfo uploadInfo = multipartService.initMultipartUpload(
                path,
                mediaType.contentType(),
                fileSize
        );

        Media media = Media.create(mediaType, path, user.getId(), fileSize, uploadInfo.uploadId());
        media = mediaRepository.save(media);

        return PresignedUrl.forMultipartUpload(media, uploadInfo.uploadId(), uploadInfo.presignedUrlParts());
    }
}
