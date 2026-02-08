package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.repost.RepostCreateRequest;
import com.apiece.springboot_sns_sample.api.repost.RepostResponse;
import com.apiece.springboot_sns_sample.config.auth.AuthUser;
import com.apiece.springboot_sns_sample.domain.post.Post;
import com.apiece.springboot_sns_sample.domain.repost.RepostService;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RepostController {

    private final RepostService repostService;

    @PostMapping("/api/v1/reposts")
    public RepostResponse createRepost(
            @RequestBody RepostCreateRequest request,
            @AuthUser User user
    ) {
        Post repost = repostService.createRepost(request.postId(), user);
        return RepostResponse.from(repost);
    }

    @GetMapping("/api/v1/reposts")
    public List<RepostResponse> getAllReposts() {
        return repostService.getAllReposts().stream()
                .map(RepostResponse::from)
                .toList();
    }

    @GetMapping("/api/v1/reposts/{id}")
    public RepostResponse getRepostById(@PathVariable Long id) {
        Post repost = repostService.getRepostById(id);
        return RepostResponse.from(repost);
    }

    @DeleteMapping("/api/v1/reposts/{id}")
    public void deleteRepost(
            @PathVariable Long id,
            @AuthUser User user
    ) {
        repostService.deleteRepost(id, user);
    }
}
