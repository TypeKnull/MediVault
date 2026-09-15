package com.medivault.repository;

import com.medivault.model.Report;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReportRepository extends MongoRepository<Report, String> {
	List<Report> findByPatientIdOrderByCreatedAtDesc(String patientId);
}
