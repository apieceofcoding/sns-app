package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.domain.postview.PostViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostViewController {

    private final PostViewService postViewService;

    @PostMapping("/api/v1/posts/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long id) {
        postViewService.incrementPostView(id);
        return ResponseEntity.noContent().build();
    }
}
