package com.apiece.springboot_sns_sample.domain.recommend;

import java.util.List;

public record RankResponse(
        Long userId,
        String segment,
        List<Long> rankedPostIds,
        Long tookMs
) {
}
