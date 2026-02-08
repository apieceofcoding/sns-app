package com.apiece.springboot_sns_sample.domain.quote;

import com.apiece.springboot_sns_sample.domain.post.Post;
import com.apiece.springboot_sns_sample.domain.post.PostRepository;
import com.apiece.springboot_sns_sample.domain.timeline.TimelineService;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final PostRepository postRepository;
    private final TimelineService timelineService;

    @Transactional
    public Post createQuote(Long quoteId, String content, User user) {
        postRepository.findByIdAndDeletedAtIsNull(quoteId)
                .orElseThrow(() -> new IllegalArgumentException("Post for quote not found: " + quoteId));

        if (postRepository.existsByUserIdAndQuoteIdAndDeletedAtIsNull(user.getId(), quoteId)) {
            throw new IllegalArgumentException("You have already quoted this post");
        }

        Post quote = Post.createQuote(content, user, quoteId);
        Post savedQuote = postRepository.save(quote);

        postRepository.incrementRepostCount(quoteId);

        timelineService.fanOutToFollowers(savedQuote.getId(), user);

        return savedQuote;
    }

    public List<Post> getAllQuotes() {
        return postRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(post -> post.getQuoteId() != null)
                .toList();
    }

    public Post getQuoteById(Long id) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found: " + id));
        if (post.getQuoteId() == null) {
            throw new IllegalArgumentException("This is not a quote");
        }
        return post;
    }

    @Transactional
    public void deleteQuote(Long id, User user) {
        Post quote = getQuoteById(id);

        if (!quote.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to delete this quote");
        }

        quote.delete();
        postRepository.decrementRepostCount(quote.getQuoteId());
    }
}
