package com.apiece.springboot_sns_sample.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedResponse(String status, String segment, List<Long> postIds, String reason) {

    public static FeedResponse success(String segment, List<Long> postIds) {
        return new FeedResponse("ok", segment, postIds, null);
    }

    public static FeedResponse failure(String segment) {
        return new FeedResponse("error", segment, null, "recommend_timeout");
    }
}
