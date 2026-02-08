package com.apiece.springboot_sns_sample.domain.timeline;

import java.util.List;

public interface TimelineRepository {

    void addPostToTimeline(Long userId, Long postId);

    /**
     * cursor 기반 타임라인 조회
     * @param userId 사용자 ID
     * @param cursor 마지막으로 본 항목의 score (null이면 처음부터)
     * @param limit 조회할 개수
     * @return TimelineEntry 리스트 (postId와 score 포함)
     */
    List<TimelineEntry> getTimeline(Long userId, Double cursor, int limit);

    void addCelebPost(Long celebUserId, Long postId);

    List<Long> getCelebPosts(Long celebUserId, int limit);

    void addPostToTimelineIfAbsent(Long userId, Long postId);
}
