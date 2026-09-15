package com.medivault.dto.device;

import com.medivault.model.enums.Platform;
import java.time.Instant;

public record DeviceTokenResponseDto(String id, String userId, String fcmToken, Platform platform, Instant lastUpdated) {
}
