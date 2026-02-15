package com.apiece.springboot_sns_sample.config.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;

@Component
@RequiredArgsConstructor
public class RustFsHealthIndicator implements HealthIndicator {

    private final S3Client s3Client;
    private final RustFsProperties rustFsProperties;

    @Override
    public Health health() {
        try {
            s3Client.listBuckets(ListBucketsRequest.builder().build());
            return Health.up()
                    .withDetail("endpoint", rustFsProperties.endpoint())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("endpoint", rustFsProperties.endpoint())
                    .withException(e)
                    .build();
        }
    }
}
