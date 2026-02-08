package com.apiece.springboot_sns_sample.api.reply;

import java.util.List;

public record ReplyCreateRequest(
        String content,
        List<Long> mediaIds
) {
}
