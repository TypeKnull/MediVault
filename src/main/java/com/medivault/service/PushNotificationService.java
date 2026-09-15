package com.medivault.service;

import com.medivault.config.FcmConfig.FcmSettings;
import com.medivault.model.DeviceToken;
import com.medivault.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationService {

	private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

	private final DeviceTokenRepository deviceTokenRepository;
	private final FcmSettings fcmSettings;

	public PushNotificationService(DeviceTokenRepository deviceTokenRepository, FcmSettings fcmSettings) {
		this.deviceTokenRepository = deviceTokenRepository;
		this.fcmSettings = fcmSettings;
	}

	public void notifyAccessRequest(String patientId, String doctorName) {
		notifyUser(patientId, "MediVault access request", doctorName + " requested access to your reports.");
	}

	public void notifyChatMessage(String recipientId, String senderName) {
		notifyUser(recipientId, "New MediVault message", senderName + " sent you a message.");
	}

	private void notifyUser(String userId, String title, String body) {
		for (DeviceToken token : deviceTokenRepository.findByUserId(userId)) {
			if (fcmSettings.configured()) {
				log.info("Queued FCM notification to {}: {} - {}", token.getFcmToken(), title, body);
			} else {
				log.info("FCM not configured; notification for user {} would be: {} - {}", userId, title, body);
			}
		}
	}
}
