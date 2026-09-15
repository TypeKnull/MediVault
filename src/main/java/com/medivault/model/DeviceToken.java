package com.medivault.model;

import com.medivault.model.enums.Platform;
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
@Document("device_tokens")
@CompoundIndex(name = "user_token_unique", def = "{'userId': 1, 'fcmToken': 1}", unique = true)
public class DeviceToken {

	@Id
	private String id;

	private String userId;

	private String fcmToken;

	private Platform platform;

	private Instant lastUpdated;
}
