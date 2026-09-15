package com.medivault.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("messages")
@CompoundIndex(name = "chat_participants_timestamp", def = "{'senderId': 1, 'receiverId': 1, 'timestamp': 1}")
public class Message {

	@Id
	private String id;

	private String senderId;

	private String receiverId;

	private String content;

	private Instant timestamp;

	private boolean read;
}
