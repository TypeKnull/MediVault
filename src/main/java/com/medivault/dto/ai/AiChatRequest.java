package com.medivault.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(@NotBlank String message, String conversationId) {
}
