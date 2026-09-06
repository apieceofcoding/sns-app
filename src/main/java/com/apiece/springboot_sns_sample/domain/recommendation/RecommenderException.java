package com.apiece.springboot_sns_sample.domain.recommendation;

public class RecommenderException extends RuntimeException {

    public RecommenderException(String message) {
        super(message);
    }

    public RecommenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
