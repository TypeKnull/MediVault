package com.medivault.repository;

import com.medivault.model.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeviceTokenRepository extends MongoRepository<DeviceToken, String> {
	List<DeviceToken> findByUserId(String userId);

	Optional<DeviceToken> findByUserIdAndFcmToken(String userId, String fcmToken);

	void deleteByUserIdAndFcmToken(String userId, String fcmToken);
}
