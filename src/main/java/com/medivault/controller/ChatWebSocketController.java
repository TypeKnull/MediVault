package com.medivault.controller;

import com.medivault.dto.chat.ChatMessageDto;
import com.medivault.model.User;
import com.medivault.service.AuthService;
import com.medivault.service.ChatService;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

	private final ChatService chatService;
	private final AuthService authService;
	private final SimpMessagingTemplate messagingTemplate;

	public ChatWebSocketController(ChatService chatService, AuthService authService, SimpMessagingTemplate messagingTemplate) {
		this.chatService = chatService;
		this.authService = authService;
		this.messagingTemplate = messagingTemplate;
	}

	@MessageMapping("/chat.send")
	public void send(Principal principal, @Payload ChatMessageDto request) {
		ChatMessageDto saved = chatService.send(principal.getName(), request);
		User receiver = authService.findById(saved.receiverId());
		messagingTemplate.convertAndSendToUser(receiver.getEmail(), "/queue/messages", saved);
		messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/messages", saved);
	}
}
