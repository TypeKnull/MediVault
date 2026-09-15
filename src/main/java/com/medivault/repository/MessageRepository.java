package com.medivault.repository;

import com.medivault.model.Message;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String> {
	List<Message> findBySenderIdAndReceiverId(String senderId, String receiverId);
}
