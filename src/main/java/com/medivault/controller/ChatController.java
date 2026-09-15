package com.medivault.controller;

import com.medivault.dto.chat.ChatHistoryResponse;
import com.medivault.dto.chat.ChatMessageDto;
import com.medivault.service.ChatService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@GetMapping("/{participantId}")
	public ChatHistoryResponse history(Principal principal, @PathVariable String participantId) {
		return chatService.history(principal.getName(), participantId);
	}

	@PostMapping("/messages")
	@ResponseStatus(HttpStatus.CREATED)
	public ChatMessageDto send(Principal principal, @Valid @RequestBody ChatMessageDto request) {
		return chatService.send(principal.getName(), request);
	}
}
