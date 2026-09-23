package com.apiece.springboot_sns_sample.controller.dto;

import java.util.List;

public record FeedResponse(String segment, List<Long> postIds) {
}
