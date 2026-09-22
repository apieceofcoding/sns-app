package com.apiece.springboot_sns_sample.api.demo;

public record ErrorResponse(
        String code,
        String message
) {
}
