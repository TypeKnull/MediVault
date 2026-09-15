package com.medivault.dto.chat;

import java.util.List;

public record ChatHistoryResponse(String participantId, List<ChatMessageDto> messages) {
}
