package com.apiece.springboot_sns_sample.domain.timeline;

import com.apiece.springboot_sns_sample.domain.follow.Follow;
import com.apiece.springboot_sns_sample.domain.follow.FollowCount;
import com.apiece.springboot_sns_sample.domain.follow.FollowCountService;
import com.apiece.springboot_sns_sample.domain.follow.FollowRepository;
import com.apiece.springboot_sns_sample.domain.post.Post;
import com.apiece.springboot_sns_sample.domain.post.PostRepository;
import com.apiece.springboot_sns_sample.domain.recommendation.RecommenderClient;
import com.apiece.springboot_sns_sample.domain.recommendation.RecommenderException;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelineRepository timelineRepository;
    private final FollowRepository followRepository;
    private final FollowCountService followCountService;
    private final PostRepository postRepository;
    private final RecommenderClient recommenderClient;

    public void fanOutToFollowers(Long postId, User author) {
        FollowCount followCount = followCountService.getFollowCountOrDefault(author.getId());

        timelineRepository.addPostToTimeline(author.getId(), postId);

        if (followCount.isCeleb()) {
            timelineRepository.addCelebPost(author.getId(), postId);
            return;
        }

        // Fanout on Write (for non-celeb users)
        List<Follow> follows = followRepository.findByFolloweeIdAndDeletedAtIsNull(author.getId());
        follows.parallelStream()
                .forEach(follow -> timelineRepository.addPostToTimeline(follow.getFollowerId(), postId));
    }

    public TimelinePage getTimeline(User user, Double cursor, int limit) {
        // Fanout on Read (for following celebs)
        List<Follow> follows = followRepository.findByFollowerIdAndDeletedAtIsNull(user.getId());
        follows.parallelStream()
                .map(Follow::getFolloweeId)
                .map(followCountService::getFollowCountOrDefault)
                .filter(FollowCount::isCeleb)
                .flatMap(followCount -> timelineRepository.getCelebPosts(followCount.getUserId(), 5).stream())
                .forEach(postId -> timelineRepository.addPostToTimelineIfAbsent(user.getId(), postId));

        List<TimelineEntry> entries = timelineRepository.getTimeline(user.getId(), cursor, limit);

        if (entries.isEmpty()) {
            return new TimelinePage(List.of(), null, false);
        }

        List<Long> postIds = entries.stream().map(TimelineEntry::postId).toList();
        Map<Long, Post> postMap = postRepository.findAllByIdInAndDeletedAtIsNull(postIds).stream()
                .collect(toMap(Post::getId, Function.identity()));

        List<Post> posts = rankForDisplay(user.getId(), postIds).stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .toList();

        Double nextCursor = entries.getLast().score();
        boolean hasMore = entries.size() >= limit;

        return new TimelinePage(posts, nextCursor, hasMore);
    }

    private List<Long> rankForDisplay(Long userId, List<Long> postIds) {
        try {
            return recommenderClient.rank(userId, postIds);
        } catch (RecommenderException e) {
            log.warn("추천 정렬에 실패해 시간순으로 응답합니다 userId={} reason={}", userId, e.getMessage());
            return postIds;
        }
    }
}
