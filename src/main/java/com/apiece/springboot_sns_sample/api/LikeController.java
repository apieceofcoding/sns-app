package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.like.LikeCreateRequest;
import com.apiece.springboot_sns_sample.api.like.LikeResponse;
import com.apiece.springboot_sns_sample.config.auth.AuthUser;
import com.apiece.springboot_sns_sample.domain.like.Like;
import com.apiece.springboot_sns_sample.domain.like.LikeService;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/api/v1/likes")
    public LikeResponse createLike(
            @RequestBody LikeCreateRequest request,
            @AuthUser User user
    ) {
        Like like = likeService.createLike(request.postId(), user);
        return LikeResponse.from(like);
    }

    @GetMapping("/api/v1/likes")
    public List<LikeResponse> getAllLikes() {
        return likeService.getAllLikes().stream()
                .map(LikeResponse::from)
                .toList();
    }

    @GetMapping("/api/v1/likes/{id}")
    public LikeResponse getLikeById(@PathVariable Long id) {
        Like like = likeService.getLikeById(id);
        return LikeResponse.from(like);
    }

    @DeleteMapping("/api/v1/likes/{id}")
    public void deleteLike(
            @PathVariable Long id,
            @AuthUser User user
    ) {
        likeService.deleteLike(id, user);
    }
}
