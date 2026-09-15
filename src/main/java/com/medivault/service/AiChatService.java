package com.medivault.service;

import com.medivault.client.AiApiClient;
import com.medivault.dto.ai.AiChatRequest;
import com.medivault.dto.ai.AiChatResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

	private final AiApiClient aiApiClient;

	public AiChatService(AiApiClient aiApiClient) {
		this.aiApiClient = aiApiClient;
	}

	public AiChatResponse chat(AiChatRequest request) {
		String conversationId = request.conversationId() == null || request.conversationId().isBlank()
				? UUID.randomUUID().toString()
				: request.conversationId();
		return aiApiClient.chat(new AiChatRequest(request.message(), conversationId));
	}
}
