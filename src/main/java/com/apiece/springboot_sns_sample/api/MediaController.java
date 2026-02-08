package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.media.MediaInitRequest;
import com.apiece.springboot_sns_sample.api.media.MediaInitResponse;
import com.apiece.springboot_sns_sample.api.media.MediaResponse;
import com.apiece.springboot_sns_sample.api.media.MediaUploadedRequest;
import com.apiece.springboot_sns_sample.api.media.PresignedUrlResponse;
import com.apiece.springboot_sns_sample.config.auth.AuthUser;
import com.apiece.springboot_sns_sample.domain.media.Media;
import com.apiece.springboot_sns_sample.domain.media.PresignedUrl;
import com.apiece.springboot_sns_sample.domain.media.MediaService;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/api/v1/media/init")
    public MediaInitResponse initMedia(
            @RequestBody MediaInitRequest request,
            @AuthUser User user
    ) {
        PresignedUrl result = mediaService.initMedia(request.mediaType(), request.fileSize(), user);
        return MediaInitResponse.from(result);
    }

    @PostMapping("/api/v1/media/uploaded")
    public MediaResponse mediaUploaded(
            @RequestBody MediaUploadedRequest request,
            @AuthUser User user
    ) {
        Media media = mediaService.mediaUploaded(request.mediaId(), request.parts(), user);
        return MediaResponse.from(media);
    }

    @GetMapping("/api/v1/media/{id}")
    public MediaResponse getMediaById(@PathVariable Long id) {
        Media media = mediaService.getMediaById(id);
        return MediaResponse.from(media);
    }

    @GetMapping("/api/v1/media/{id}/presigned-url")
    public PresignedUrlResponse getPresignedUrl(@PathVariable Long id) {
        Media media = mediaService.getMediaById(id);
        String presignedUrl = mediaService.getPresignedUrl(media);
        return new PresignedUrlResponse(presignedUrl, MediaResponse.from(media));
    }

    @GetMapping("/api/v1/users/{userId}/media")
    public List<MediaResponse> getMediaByUserId(@PathVariable Long userId) {
        return mediaService.getMediaByUserId(userId).stream()
                .map(MediaResponse::from)
                .toList();
    }

    @DeleteMapping("/api/v1/media/{id}")
    public void deleteMedia(
            @PathVariable Long id,
            @AuthUser User user
    ) {
        mediaService.deleteMedia(id, user);
    }
}
