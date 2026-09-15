package com.medivault.dto.chat;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record ChatMessageDto(
		String id,
		String senderId,
		@NotBlank String receiverId,
		@NotBlank String content,
		Instant timestamp,
		boolean read) {
}
