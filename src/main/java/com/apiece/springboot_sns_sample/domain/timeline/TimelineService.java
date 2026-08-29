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

        // 다음 페이지 기준점은 재정렬 전의 시간순 결과에서 뽑는다. 추천 순서로 뽑으면
        // 마지막 항목이 가장 오래된 글이라는 보장이 깨져서 페이지가 밀리거나 겹친다.
        Double nextCursor = entries.getLast().score();
        boolean hasMore = entries.size() >= limit;

        return new TimelinePage(posts, nextCursor, hasMore);
    }

    /**
     * 한 페이지 안에서만 노출 순서를 바꾼다. 페이지 경계는 시간순이 정하고, 그 안의 배열은 추천이 정한다.
     *
     * <p>추천 서비스가 느리거나 죽어도 타임라인 자체는 보여야 하므로, 실패하면 원래 시간순으로 돌아간다.
     */
    private List<Long> rankForDisplay(Long userId, List<Long> postIds) {
        try {
            return recommenderClient.rank(userId, postIds);
        } catch (RecommenderException e) {
            log.warn("추천 정렬에 실패해 시간순으로 응답합니다 userId={} reason={}", userId, e.getMessage());
            return postIds;
        }
    }
}
