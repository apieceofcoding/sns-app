package com.apiece.springboot_sns_sample.api.demo;

import java.util.List;

public record TraceResponse(
        String message,
        List<Long> rankedPostIds
) {
}
