package com.apiece.springboot_sns_sample.domain.postview;

import com.apiece.springboot_sns_sample.domain.post.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostViewSyncScheduler {

    private final PostViewRepository postViewRepository;
    private final PostRepository postRepository;

    @Scheduled(fixedRate = 60000) // every 1 minute
    @Transactional
    public void syncPostViewsToDatabase() {
        Set<Long> dirtyPostIds = postViewRepository.getDirtyPostIds();

        if (dirtyPostIds.isEmpty()) {
            return;
        }

        log.info("Syncing post views for {} posts", dirtyPostIds.size());

        dirtyPostIds.forEach(postId -> {
            try {
                Long postView = postViewRepository.getPostView(postId);

                postRepository.findByIdAndDeletedAtIsNull(postId).ifPresent(post -> {
                    post.updateViewCount(postView);
                    postRepository.save(post);
                });

                postViewRepository.removeDirtyPostId(postId);
            } catch (Exception e) {
                log.error("Failed to sync view count for post {}", postId, e);
            }
        });
    }
}
