package com.apiece.springboot_sns_sample.api.post;

import java.util.List;

public record PostCreateRequest(
        String content,
        List<Long> mediaIds
) {
}
