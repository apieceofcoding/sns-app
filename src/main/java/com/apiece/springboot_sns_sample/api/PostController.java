package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.post.PostCreateRequest;
import com.apiece.springboot_sns_sample.api.post.PostResponse;
import com.apiece.springboot_sns_sample.api.post.PostUpdateRequest;
import com.apiece.springboot_sns_sample.config.auth.AuthUser;
import com.apiece.springboot_sns_sample.domain.post.Post;
import com.apiece.springboot_sns_sample.domain.post.PostService;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/api/v1/posts")
    public PostResponse createPost(
            @RequestBody PostCreateRequest request,
            @AuthUser User user
    ) {
        Post post = postService.createPost(request.content(), request.mediaIds(), user);
        return PostResponse.from(post);
    }

    @GetMapping("/api/v1/posts")
    public List<PostResponse> getAllPosts() {
        return postService.getAllPosts().stream()
                .map(PostResponse::from)
                .toList();
    }

    @GetMapping("/api/v1/posts/{id}")
    public PostResponse getPostById(
            @PathVariable Long id,
            @AuthUser User user
    ) {
        Post post = postService.getPostById(id);
        return PostResponse.from(postService.enrichWithUserContext(post, user));
    }

    @PutMapping("/api/v1/posts/{id}")
    public PostResponse updatePost(
            @PathVariable Long id,
            @RequestBody PostUpdateRequest request,
            @AuthUser User user
    ) {
        Post post = postService.updatePost(id, request.content(), user);
        return PostResponse.from(post);
    }

    @DeleteMapping("/api/v1/posts/{id}")
    public void deletePost(
            @PathVariable Long id,
            @AuthUser User user
    ) {
        postService.deletePost(id, user);
    }

}
