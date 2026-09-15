package com.medivault.model;

import com.medivault.model.enums.PermissionStatus;
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
@Document("permission_requests")
@CompoundIndex(name = "doctor_patient_unique", def = "{'doctorId': 1, 'patientId': 1}", unique = true)
public class PermissionRequest {

	@Id
	private String id;

	private String doctorId;

	private String patientId;

	private PermissionStatus status;

	private Instant requestedAt;

	private Instant respondedAt;
}
