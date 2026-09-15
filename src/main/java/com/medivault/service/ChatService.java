package com.medivault.service;

import com.medivault.dto.chat.ChatHistoryResponse;
import com.medivault.dto.chat.ChatMessageDto;
import com.medivault.exception.PermissionDeniedException;
import com.medivault.model.Message;
import com.medivault.model.User;
import com.medivault.model.enums.Role;
import com.medivault.repository.MessageRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

	private final MessageRepository messageRepository;
	private final AuthService authService;
	private final PermissionService permissionService;
	private final PushNotificationService pushNotificationService;

	public ChatService(
			MessageRepository messageRepository,
			AuthService authService,
			PermissionService permissionService,
			PushNotificationService pushNotificationService) {
		this.messageRepository = messageRepository;
		this.authService = authService;
		this.permissionService = permissionService;
		this.pushNotificationService = pushNotificationService;
	}

	public ChatMessageDto send(String senderEmail, ChatMessageDto request) {
		User sender = authService.findByEmail(senderEmail);
		User receiver = authService.findById(request.receiverId());
		ensureChatAllowed(sender, receiver);

		Message message = Message.builder()
				.senderId(sender.getId())
				.receiverId(receiver.getId())
				.content(request.content())
				.timestamp(Instant.now())
				.read(false)
				.build();

		Message saved = messageRepository.save(message);
		pushNotificationService.notifyChatMessage(receiver.getId(), sender.getName());
		return toDto(saved);
	}

	public ChatHistoryResponse history(String email, String participantId) {
		User currentUser = authService.findByEmail(email);
		User participant = authService.findById(participantId);
		ensureChatAllowed(currentUser, participant);

		List<ChatMessageDto> messages = Stream.concat(
						messageRepository.findBySenderIdAndReceiverId(currentUser.getId(), participant.getId()).stream(),
						messageRepository.findBySenderIdAndReceiverId(participant.getId(), currentUser.getId()).stream())
				.sorted(Comparator.comparing(Message::getTimestamp))
				.map(this::toDto)
				.toList();

		return new ChatHistoryResponse(participant.getId(), messages);
	}

	private void ensureChatAllowed(User first, User second) {
		if (first.getRole() == second.getRole()) {
			throw new PermissionDeniedException("Chat is available between doctors and patients");
		}
		User doctor = first.getRole() == Role.DOCTOR ? first : second;
		User patient = first.getRole() == Role.PATIENT ? first : second;
		if (!permissionService.hasApprovedAccess(doctor.getId(), patient.getId())) {
			throw new PermissionDeniedException("Patient approval is required before chat");
		}
	}

	private ChatMessageDto toDto(Message message) {
		return new ChatMessageDto(
				message.getId(),
				message.getSenderId(),
				message.getReceiverId(),
				message.getContent(),
				message.getTimestamp(),
				message.isRead());
	}
}
