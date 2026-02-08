package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.post.PostResponse;

import java.util.List;

public record TimelineResponse(
        List<PostResponse> posts,
        Double nextCursor,
        boolean hasMore
) {
}
