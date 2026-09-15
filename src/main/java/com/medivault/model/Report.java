package com.medivault.model;

import com.medivault.model.enums.ReportStatus;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("reports")
public class Report {

	@Id
	private String id;

	@Indexed
	private String patientId;

	private String originalFileName;

	private String contentType;

	private String fileUrl;

	private String extractedText;

	private String documentType;

	private Map<String, String> fields;

	private String aiSummary;

	private ReportStatus status;

	@CreatedDate
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;
}
