package com.medivault.repository;

import com.medivault.model.PermissionRequest;
import com.medivault.model.enums.PermissionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PermissionRepository extends MongoRepository<PermissionRequest, String> {
	Optional<PermissionRequest> findByDoctorIdAndPatientId(String doctorId, String patientId);

	List<PermissionRequest> findByPatientIdOrderByRequestedAtDesc(String patientId);

	List<PermissionRequest> findByDoctorIdAndStatus(String doctorId, PermissionStatus status);
}
