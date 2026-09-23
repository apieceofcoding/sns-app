package com.apiece.springboot_sns_sample.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedResponse(String segment, List<Long> postIds) {
}
