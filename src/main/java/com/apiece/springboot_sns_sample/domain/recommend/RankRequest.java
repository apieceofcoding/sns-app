package com.apiece.springboot_sns_sample.domain.recommend;

import java.util.List;

public record RankRequest(
        Long userId,
        List<Long> postIds
) {
}
