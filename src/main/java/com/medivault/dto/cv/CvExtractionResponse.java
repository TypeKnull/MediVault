package com.medivault.dto.cv;

import java.util.Map;

public record CvExtractionResponse(String extractedText, String documentType, Map<String, String> fields) {
	public CvExtractionResponse(String extractedText) {
		this(extractedText, "UNKNOWN", Map.of());
	}
}
