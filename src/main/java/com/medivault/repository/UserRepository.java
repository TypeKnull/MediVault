package com.medivault.repository;

import com.medivault.model.User;
import com.medivault.model.enums.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	List<User> findByRole(Role role);

	List<User> findByRoleAndSpecializationContainingIgnoreCase(Role role, String specialization);
}
