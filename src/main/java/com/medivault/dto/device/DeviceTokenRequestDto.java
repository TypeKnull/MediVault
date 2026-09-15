package com.medivault.dto.device;

import com.medivault.model.enums.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceTokenRequestDto(@NotBlank String fcmToken, @NotNull Platform platform) {
}
