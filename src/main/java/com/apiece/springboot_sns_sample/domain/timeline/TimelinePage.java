package com.apiece.springboot_sns_sample.domain.timeline;

import com.apiece.springboot_sns_sample.domain.post.Post;

import java.util.List;

public record TimelinePage(
        List<Post> posts,
        Double nextCursor,
        boolean hasMore
) {
}
