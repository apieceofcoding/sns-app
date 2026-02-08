package com.apiece.springboot_sns_sample.api;

import com.apiece.springboot_sns_sample.api.quote.QuoteCreateRequest;
import com.apiece.springboot_sns_sample.api.quote.QuoteResponse;
import com.apiece.springboot_sns_sample.config.auth.AuthUser;
import com.apiece.springboot_sns_sample.domain.post.Post;
import com.apiece.springboot_sns_sample.domain.quote.QuoteService;
import com.apiece.springboot_sns_sample.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/api/v1/posts/{postId}/quotes")
    public QuoteResponse createQuote(
            @PathVariable Long postId,
            @RequestBody QuoteCreateRequest request,
            @AuthUser User user
    ) {
        Post quote = quoteService.createQuote(postId, request.content(), user);
        return QuoteResponse.from(quote);
    }

    @GetMapping("/api/v1/quotes")
    public List<QuoteResponse> getAllQuotes() {
        return quoteService.getAllQuotes().stream()
                .map(QuoteResponse::from)
                .toList();
    }

    @GetMapping("/api/v1/quotes/{id}")
    public QuoteResponse getQuoteById(@PathVariable Long id) {
        Post quote = quoteService.getQuoteById(id);
        return QuoteResponse.from(quote);
    }

    @DeleteMapping("/api/v1/quotes/{id}")
    public void deleteQuote(
            @PathVariable Long id,
            @AuthUser User user
    ) {
        quoteService.deleteQuote(id, user);
    }
}
