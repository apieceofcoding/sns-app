package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.reply.ReplyCreateRequest;
import com.apiece.springboot_sns_sample.api.reply.ReplyResponse;
import com.apiece.springboot_sns_sample.api.reply.ReplyUpdateRequest;
import com.apiece.springboot_sns_sample.config.auth.AuthUser;
import com.apiece.springboot_sns_sample.domain.post.Post;
import com.apiece.springboot_sns_sample.domain.reply.ReplyService;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService replyService;

    @PostMapping("/api/v1/posts/{postId}/replies")
    public ReplyResponse createReply(
            @PathVariable Long postId,
            @RequestBody ReplyCreateRequest request,
            @AuthUser User user
    ) {
        Post reply = replyService.createReply(postId, request.content(), request.mediaIds(), user);
        return ReplyResponse.from(reply);
    }

    @GetMapping("/api/v1/posts/{postId}/replies")
    public List<ReplyResponse> getReplies(@PathVariable Long postId) {
        return replyService.getRepliesByParentId(postId).stream()
                .map(ReplyResponse::from)
                .toList();
    }

    @PutMapping("/api/v1/replies/{replyId}")
    public ReplyResponse updateReply(
            @PathVariable Long replyId,
            @RequestBody ReplyUpdateRequest request,
            @AuthUser User user
    ) {
        Post reply = replyService.updateReply(replyId, request.content(), user);
        return ReplyResponse.from(reply);
    }

    @DeleteMapping("/api/v1/replies/{replyId}")
    public void deleteReply(
            @PathVariable Long replyId,
            @AuthUser User user
    ) {
        replyService.deleteReply(replyId, user);
    }
}
