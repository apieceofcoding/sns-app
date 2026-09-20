package com.apiece.springboot_sns_sample.api.demo;

public record ErrorResponse(
        String status,
        String message
) {
}
