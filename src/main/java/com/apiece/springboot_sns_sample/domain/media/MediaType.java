package com.apiece.springboot_sns_sample.domain.media;

public enum MediaType {
    IMAGE,
    VIDEO;

    public String fileExtension() {
        return switch (this) {
            case IMAGE -> ".jpg";
            case VIDEO -> ".mp4";
        };
    }

    public String contentType() {
        return switch (this) {
            case IMAGE -> "image/jpeg";
            case VIDEO -> "video/mp4";
        };
    }
}
