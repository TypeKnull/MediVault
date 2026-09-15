package com.medivault.client;

import com.medivault.dto.ai.AiChatRequest;
import com.medivault.dto.ai.AiChatResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AiApiClient {

	private final WebClient.Builder webClientBuilder;
	private final String aiApiUrl;
	private final String aiApiKey;
	private final String aiModel;

	public AiApiClient(
			WebClient.Builder webClientBuilder,
			@Value("${medivault.external.ai-api-url:}") String aiApiUrl,
			@Value("${medivault.external.ai-api-key:}") String aiApiKey,
			@Value("${medivault.external.ai-model:gemini-3.6-flash}") String aiModel) {
		this.webClientBuilder = webClientBuilder;
		this.aiApiUrl = aiApiUrl;
		this.aiApiKey = aiApiKey;
		this.aiModel = aiModel;
	}

	public AiChatResponse chat(AiChatRequest request) {
		if (!configured()) {
			return new AiChatResponse(request.conversationId(), "AI service is not configured yet.");
		}
		try {
			String reply = callGemini(medivaultSystemInstruction(), request.message(), 2000);
			return new AiChatResponse(request.conversationId(), reply);
		} catch (RuntimeException exception) {
			return new AiChatResponse(request.conversationId(), "AI chat failed. Please try again later.");
		}
	}

	public String summarize(String extractedText) {
		if (extractedText == null || extractedText.isBlank()) {
			return "No extractable text was available for this report.";
		}
		if (!configured()) {
			return extractedText.length() <= 500 ? extractedText : extractedText.substring(0, 500) + "...";
		}
		try {
			return callGemini(summarySystemInstruction(), "Extracted report text:\n" + extractedText, 700);
		} catch (RuntimeException exception) {
			return "AI summary failed. Please try again later.";
		}
	}

	public String summarizeReport(String documentType, Map<String, String> fields, String extractedText) {
		if ((extractedText == null || extractedText.isBlank()) && (fields == null || fields.isEmpty())) {
			return "No extractable report data was available for summarization.";
		}
		if (!configured()) {
			return fallbackReportSummary(documentType, fields, extractedText);
		}
		try {
			return callGemini(summarySystemInstruction(), reportSummaryPrompt(documentType, fields, extractedText), 2000);
		} catch (RuntimeException exception) {
			return "AI summary failed. Please try again later.";
		}
	}

	private String callGemini(String systemInstruction, String userPrompt, int maxOutputTokens) {
		Map<String, Object> requestBody = Map.of(
				"systemInstruction", Map.of(
						"role", "system",
						"parts", List.of(Map.of("text", systemInstruction))),
				"contents", List.of(Map.of(
						"role", "user",
						"parts", List.of(Map.of("text", userPrompt)))),
				"generationConfig", Map.of(
						"temperature", 0.2,
						"maxOutputTokens", maxOutputTokens));

		Map<?, ?> response = webClientBuilder.build()
				.post()
				.uri(geminiEndpoint())
				.bodyValue(requestBody)
				.retrieve()
				.bodyToMono(Map.class)
				.block(Duration.ofSeconds(30));

		return extractGeminiText(response);
	}

	private String extractGeminiText(Map<?, ?> response) {
		if (response == null) {
			return "AI returned an empty response.";
		}
		Object candidatesValue = response.get("candidates");
		if (candidatesValue instanceof List<?> candidates && !candidates.isEmpty()
				&& candidates.getFirst() instanceof Map<?, ?> candidate
				&& candidate.get("content") instanceof Map<?, ?> content
				&& content.get("parts") instanceof List<?> parts) {
			StringBuilder text = new StringBuilder();
			for (Object part : parts) {
				if (part instanceof Map<?, ?> partMap && partMap.get("text") != null) {
					text.append(partMap.get("text"));
				}
			}
			if (!text.toString().isBlank()) {
				return text.toString().trim();
			}
		}
		return "AI returned an empty response.";
	}

	private String geminiEndpoint() {
		String baseUrl = aiApiUrl.endsWith("/") ? aiApiUrl.substring(0, aiApiUrl.length() - 1) : aiApiUrl;
		return baseUrl + "/models/" + aiModel + ":generateContent?key=" + aiApiKey;
	}

	private boolean configured() {
		return aiApiUrl != null && !aiApiUrl.isBlank()
				&& aiApiKey != null && !aiApiKey.isBlank()
				&& aiModel != null && !aiModel.isBlank();
	}

	private String medivaultSystemInstruction() {
		return """
				You are MediVault's medical-record assistant. Explain information contained in uploaded medical records in simple language. Help users understand medical terminology and distinguish extracted record data from general explanation. Do not invent report values. Do not diagnose diseases. Do not prescribe medication or treatment. Recommend consulting a qualified healthcare professional for diagnosis or treatment decisions.
				""";
	}

	private String summarySystemInstruction() {
		return """
				You are MediVault's report summarization assistant. Produce concise, doctor-readable summaries using only the supplied report data. Do not invent missing values. Do not diagnose the patient. Do not make treatment recommendations.
				""";
	}

	private String reportSummaryPrompt(String documentType, Map<String, String> fields, String extractedText) {
		return """
				Generate a concise factual summary in this structure:

				Report Type: <human readable type>

				Key Results:
				- <field>: <value>

				Summary:
				<short factual summary based only on supplied data>

				Document type: %s
				Structured fields: %s
				Extracted OCR text:
				%s
				""".formatted(
				documentType == null || documentType.isBlank() ? "UNKNOWN" : documentType,
				fields == null ? Map.of() : fields,
				extractedText == null ? "" : extractedText);
	}

	private String fallbackReportSummary(String documentType, Map<String, String> fields, String extractedText) {
		StringBuilder summary = new StringBuilder();
		summary.append("Report Type: ")
				.append(documentType == null || documentType.isBlank() ? "UNKNOWN" : documentType)
				.append("\n\nKey Results:\n");
		if (fields == null || fields.isEmpty()) {
			summary.append("- No structured fields were extracted.\n");
		} else {
			fields.forEach((key, value) -> summary.append("- ").append(key).append(": ").append(value).append("\n"));
		}
		summary.append("\nSummary:\nAI service is not configured yet.");
		if (extractedText != null && !extractedText.isBlank()) {
			summary.append(" Extracted text is available in the report record.");
		}
		return summary.toString();
	}
}
