package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.media.MediaInitResponse;
import com.apiece.springboot_sns_sample.api.post.PostResponse;
import com.apiece.springboot_sns_sample.api.reply.ReplyResponse;
import com.apiece.springboot_sns_sample.api.user.ProfileImageInitRequest;
import com.apiece.springboot_sns_sample.api.user.ProfileImageUpdateRequest;
import com.apiece.springboot_sns_sample.api.user.UserResponse;
import com.apiece.springboot_sns_sample.config.auth.AuthUser;
import com.apiece.springboot_sns_sample.domain.like.LikeService;
import com.apiece.springboot_sns_sample.domain.media.PresignedUrl;
import com.apiece.springboot_sns_sample.domain.post.PostService;
import com.apiece.springboot_sns_sample.domain.post.PostWithViewCount;
import com.apiece.springboot_sns_sample.domain.user.User;
import com.apiece.springboot_sns_sample.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final PostService postService;
    private final LikeService likeService;
    private final UserService userService;

    @GetMapping("/api/v1/profile/posts")
    public List<PostResponse> getMyPosts(@AuthUser User user) {
        List<PostWithViewCount> postsWithViewCount = new ArrayList<>(postService.getPostsByUserId(user.getId()));

        return postsWithViewCount.stream()
                .sorted(comparing(pvc -> pvc.post().getCreatedAt(), reverseOrder()))
                .map(pvc -> postService.enrichWithUserContext(pvc.post(), user))
                .map(PostResponse::from)
                .toList();
    }

    @GetMapping("/api/v1/profile/replies")
    public List<ReplyResponse> getMyReplies(@AuthUser User user) {
        return postService.getRepliesByUserId(user.getId()).stream()
                .map(ReplyResponse::from)
                .toList();
    }

    @GetMapping("/api/v1/profile/likes")
    public List<PostResponse> getMyLikedPosts(@AuthUser User user) {
        return likeService.getLikesByUserId(user.getId()).stream()
                .map(like -> postService.enrichWithUserContext(like.getPost(), user))
                .map(PostResponse::from)
                .toList();
    }

    @PostMapping("/api/v1/profile/image/init")
    public MediaInitResponse initProfileImage(@AuthUser User user, @RequestBody ProfileImageInitRequest request) {
        PresignedUrl presignedUrl = userService.initProfileImage(request.fileSize(), user);
        return MediaInitResponse.from(presignedUrl);
    }

    @PostMapping("/api/v1/profile/image/uploaded")
    public UserResponse uploadedProfileImage(@AuthUser User user, @RequestBody ProfileImageUpdateRequest request) {
        User updatedUser = userService.updateProfileImage(request.mediaId(), user);
        String profileImageUrl = userService.getProfileImageUrl(updatedUser);
        return UserResponse.from(updatedUser, profileImageUrl);
    }
}
