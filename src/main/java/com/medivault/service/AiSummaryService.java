package com.medivault.service;

import com.medivault.client.AiApiClient;
import com.medivault.dto.report.ReportResponse;
import org.springframework.stereotype.Service;

@Service
public class AiSummaryService {

	private final AiApiClient aiApiClient;

	public AiSummaryService(AiApiClient aiApiClient) {
		this.aiApiClient = aiApiClient;
	}

	public String summarize(String extractedText) {
		return aiApiClient.summarize(extractedText);
	}

	public String summarizeReport(ReportResponse report) {
		return aiApiClient.summarizeReport(report.documentType(), report.fields(), report.extractedText());
	}
}
