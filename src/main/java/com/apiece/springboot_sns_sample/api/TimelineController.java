package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.post.PostResponse;
import com.apiece.springboot_sns_sample.config.auth.AuthUser;
import com.apiece.springboot_sns_sample.domain.post.PostService;
import com.apiece.springboot_sns_sample.domain.timeline.TimelinePage;
import com.apiece.springboot_sns_sample.domain.timeline.TimelineService;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;
    private final PostService postService;

    @GetMapping("/api/v1/timelines")
    public TimelineResponse getTimeline(
            @AuthUser User user,
            @RequestParam(required = false) Double cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        TimelinePage page = timelineService.getTimeline(user, cursor, limit);
        List<PostResponse> posts = postService.enrichWithUserContext(page.posts(), user).stream()
                .map(PostResponse::from)
                .toList();
        return new TimelineResponse(posts, page.nextCursor(), page.hasMore());
    }
}
