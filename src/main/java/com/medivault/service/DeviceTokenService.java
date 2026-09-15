package com.medivault.service;

import com.medivault.dto.device.DeviceTokenRequestDto;
import com.medivault.dto.device.DeviceTokenResponseDto;
import com.medivault.model.DeviceToken;
import com.medivault.model.User;
import com.medivault.repository.DeviceTokenRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class DeviceTokenService {

	private final DeviceTokenRepository deviceTokenRepository;
	private final AuthService authService;

	public DeviceTokenService(DeviceTokenRepository deviceTokenRepository, AuthService authService) {
		this.deviceTokenRepository = deviceTokenRepository;
		this.authService = authService;
	}

	public DeviceTokenResponseDto register(String email, DeviceTokenRequestDto request) {
		User user = authService.findByEmail(email);
		DeviceToken token = deviceTokenRepository.findByUserIdAndFcmToken(user.getId(), request.fcmToken())
				.orElseGet(DeviceToken::new);
		token.setUserId(user.getId());
		token.setFcmToken(request.fcmToken());
		token.setPlatform(request.platform());
		token.setLastUpdated(Instant.now());
		return toResponse(deviceTokenRepository.save(token));
	}

	public void unregister(String email, DeviceTokenRequestDto request) {
		User user = authService.findByEmail(email);
		deviceTokenRepository.deleteByUserIdAndFcmToken(user.getId(), request.fcmToken());
	}

	private DeviceTokenResponseDto toResponse(DeviceToken token) {
		return new DeviceTokenResponseDto(
				token.getId(),
				token.getUserId(),
				token.getFcmToken(),
				token.getPlatform(),
				token.getLastUpdated());
	}
}
