package com.medivault.client;

import com.medivault.dto.cv.CvExtractionRequest;
import com.medivault.dto.cv.CvExtractionResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CvServiceClient {

	private final WebClient.Builder webClientBuilder;
	private final String cvServiceUrl;
	private final String ocrSpaceApiUrl;
	private final String ocrSpaceApiKey;

	public CvServiceClient(
			WebClient.Builder webClientBuilder,
			@Value("${medivault.external.cv-service-url:}") String cvServiceUrl,
			@Value("${medivault.external.ocr-space-api-url}") String ocrSpaceApiUrl,
			@Value("${medivault.external.ocr-space-api-key:}") String ocrSpaceApiKey) {
		this.webClientBuilder = webClientBuilder;
		this.cvServiceUrl = cvServiceUrl;
		this.ocrSpaceApiUrl = ocrSpaceApiUrl;
		this.ocrSpaceApiKey = ocrSpaceApiKey;
	}

	public CvExtractionResponse extract(String fileUrl) {
		if (hasText(cvServiceUrl)) {
			return extractWithExternalCvService(fileUrl);
		}
		if (!hasText(ocrSpaceApiKey)) {
			return new CvExtractionResponse(
					"OCR.space API key is not configured. File stored at: " + fileUrl,
					"UNKNOWN",
					Map.of());
		}

		try {
			String extractedText = callOcrSpace(fileUrl);
			return new CvExtractionResponse(
					extractedText,
					detectDocumentType(extractedText),
					extractStructuredFields(extractedText));
		} catch (RuntimeException exception) {
			return new CvExtractionResponse("OCR extraction failed: " + exception.getMessage(), "UNKNOWN", Map.of());
		}
	}

	private CvExtractionResponse extractWithExternalCvService(String fileUrl) {
		try {
			CvExtractionResponse response = webClientBuilder.build()
					.post()
					.uri(cvServiceUrl)
					.bodyValue(new CvExtractionRequest(fileUrl))
					.retrieve()
					.bodyToMono(CvExtractionResponse.class)
					.block(Duration.ofSeconds(30));
			if (response == null) {
				return new CvExtractionResponse("", "UNKNOWN", Map.of());
			}
			return response;
		} catch (RuntimeException exception) {
			return new CvExtractionResponse("CV extraction failed: " + exception.getMessage());
		}
	}

	private String callOcrSpace(String fileUrl) {
		Path path = Path.of(fileUrl);
		if (!Files.exists(path)) {
			throw new IllegalArgumentException("Stored file does not exist");
		}

		try {
			String contentType = Files.probeContentType(path);
			if (!hasText(contentType)) {
				contentType = "image/png";
			}
			String base64Image = Base64.getEncoder().encodeToString(Files.readAllBytes(path));

			MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
			formData.add("apikey", ocrSpaceApiKey);
			formData.add("base64Image", "data:" + contentType + ";base64," + base64Image);
			formData.add("language", "eng");
			formData.add("isOverlayRequired", "false");
			formData.add("OCREngine", "2");

			Map<?, ?> response = webClientBuilder.build()
					.post()
					.uri(ocrSpaceApiUrl)
					.bodyValue(formData)
					.retrieve()
					.bodyToMono(Map.class)
					.block(Duration.ofSeconds(45));

			return extractOcrSpaceText(response);
		} catch (Exception exception) {
			throw new IllegalStateException(exception.getMessage(), exception);
		}
	}

	private String extractOcrSpaceText(Map<?, ?> response) {
		if (response == null) {
			return "";
		}
		Object errored = response.get("IsErroredOnProcessing");
		if (Boolean.TRUE.equals(errored)) {
			throw new IllegalArgumentException(readErrorMessage(response.get("ErrorMessage")));
		}
		Object parsedResults = response.get("ParsedResults");
		if (parsedResults instanceof List<?> results && !results.isEmpty() && results.getFirst() instanceof Map<?, ?> first) {
			Object parsedText = first.get("ParsedText");
			return parsedText == null ? "" : parsedText.toString().trim();
		}
		return "";
	}

	private String readErrorMessage(Object errorMessage) {
		if (errorMessage instanceof List<?> errors) {
			return String.join(", ", errors.stream().map(String::valueOf).toList());
		}
		return errorMessage == null ? "Unknown OCR.space API error" : errorMessage.toString();
	}

	String detectDocumentType(String extractedText) {
		String text = extractedText == null ? "" : extractedText.toLowerCase(Locale.ROOT);
		int labEvidence = evidenceScore(text,
				"\\blab\\s*report\\b",
				"\\bblood\\s*test\\b",
				"\\breference\\s*range\\b",
				"\\b(?:ha?emoglobin|hgb)\\b",
				"\\bglucose\\b",
				"\\bcholesterol\\b",
				"\\bplatelets?\\b",
				"\\brbc\\b",
				"\\bwbc\\b",
				"\\bcreatinine\\b",
				"\\btriglycerides?\\b",
				"\\bhematocrit\\b");
		int prescriptionEvidence = evidenceScore(text,
				"\\bprescription\\b",
				"\\brx\\b",
				"\\bdosage\\b",
				"\\btablets?\\b",
				"\\bcapsules?\\b",
				"\\bmedicine\\b",
				"\\bmedication\\b",
				"\\bprescribed\\b",
				"\\btake\\s+\\d+\\b",
				"\\b(?:once|twice|thrice)\\s+(?:daily|a\\s+day)\\b");

		if (labEvidence >= 2 && labEvidence > prescriptionEvidence) {
			return "LAB_REPORT";
		}
		if (prescriptionEvidence >= 2 && prescriptionEvidence > labEvidence) {
			return "PRESCRIPTION";
		}
		return "UNKNOWN";
	}

	Map<String, String> extractStructuredFields(String extractedText) {
		if (!hasText(extractedText)) {
			return Map.of();
		}
		String documentType = detectDocumentType(extractedText);
		if ("LAB_REPORT".equals(documentType)) {
			return extractLabFields(extractedText);
		}
		if ("PRESCRIPTION".equals(documentType)) {
			return extractPrescriptionFields(extractedText);
		}
		return Map.of();
	}

	private Map<String, String> extractLabFields(String extractedText) {
		Map<String, String> fields = new LinkedHashMap<>();
		List<String> lines = extractedText.lines()
				.map(String::trim)
				.filter(line -> !line.isBlank())
				.toList();
		Set<Integer> usedValueLines = new HashSet<>();

		addLabField(fields, usedValueLines, "hemoglobin", lines, Pattern.compile("\\b(?:ha?em[ao]globin|h[ae]m[ao]globin|hgb|hb|hemo\\s*lomin)\\b", Pattern.CASE_INSENSITIVE), 6, 2, true);
		addLabField(fields, usedValueLines, "glucose", lines, Pattern.compile("\\bglucose\\b", Pattern.CASE_INSENSITIVE), 0, 2, false);
		addLabField(fields, usedValueLines, "cholesterol", lines, Pattern.compile("\\bcholesterol\\b", Pattern.CASE_INSENSITIVE), 0, 2, false);
		addLabField(fields, usedValueLines, "creatinine", lines, Pattern.compile("\\bcreatinine\\b", Pattern.CASE_INSENSITIVE), 0, 2, false);
		addLabField(fields, usedValueLines, "platelet_count", lines, Pattern.compile("\\b(?:platelet\\s*count|platelets?|poster\\s*coun)\\b", Pattern.CASE_INSENSITIVE), 0, 4, false);
		addLabField(fields, usedValueLines, "rbc", lines, Pattern.compile("\\b(?:rbc|red\\s*blood\\s*cells?|total\\s*rbc\\s*cou?st|total\\s*rbc\\s*count)\\b", Pattern.CASE_INSENSITIVE), 0, 3, false);
		addLabField(fields, usedValueLines, "wbc", lines, Pattern.compile("\\b(?:wbc|white\\s*blood\\s*cells?|total\\s*wbc\\s*count)\\b", Pattern.CASE_INSENSITIVE), 0, 3, false);
		addLabField(fields, usedValueLines, "pcv", lines, Pattern.compile("\\b(?:pcv|packed\\s*cell\\s*volume)\\b", Pattern.CASE_INSENSITIVE), 0, 8, false);
		addLabField(fields, usedValueLines, "mcv", lines, Pattern.compile("\\b(?:mcv|mcw|mean\\s*corpuscul\\w*\\s*volume)\\b", Pattern.CASE_INSENSITIVE), 0, 8, false);
		addLabField(fields, usedValueLines, "mch", lines, Pattern.compile("\\bmch\\b", Pattern.CASE_INSENSITIVE), 0, 1, false);
		addLabField(fields, usedValueLines, "mchc", lines, Pattern.compile("\\b(?:mchc|mchs)\\b", Pattern.CASE_INSENSITIVE), 0, 1, false);
		addLabField(fields, usedValueLines, "rdw", lines, Pattern.compile("\\b(?:rdw|row)\\b", Pattern.CASE_INSENSITIVE), 0, 1, false);
		addLabField(fields, usedValueLines, "lymphocytes", lines, Pattern.compile("\\b(?:lymphocytes|lympliocytes)\\b", Pattern.CASE_INSENSITIVE), 0, 1, false);
		addLabField(fields, usedValueLines, "monocytes", lines, Pattern.compile("\\b(?:monocytes|monorses)\\b", Pattern.CASE_INSENSITIVE), 0, 1, false);
		addLabField(fields, usedValueLines, "eosinophils", lines, Pattern.compile("\\b(?:eosinophils|eosinophil|eos)\\b", Pattern.CASE_INSENSITIVE), 0, 1, false);
		addLabField(fields, usedValueLines, "basophils", lines, Pattern.compile("\\bbasophils\\b", Pattern.CASE_INSENSITIVE), 0, 1, false);
		addFirstMatch(fields, "reportDate", extractedText, "\\b(?:date|report\\s*date)\\b\\s*[:\\-]?\\s*([0-9]{1,2}[/-][0-9]{1,2}[/-][0-9]{2,4})");
		return fields;
	}

	private void addLabField(
			Map<String, String> fields,
			Set<Integer> usedValueLines,
			String key,
			List<String> lines,
			Pattern labelPattern,
			int maxLinesBefore,
			int maxLinesAfter,
			boolean preferPreviousValue) {
		if (fields.containsKey(key)) {
			return;
		}
		for (int index = 0; index < lines.size(); index++) {
			Matcher labelMatcher = labelPattern.matcher(lines.get(index));
			if (!labelMatcher.find()) {
				continue;
			}

			LabValue sameLineValue = valueAfterLabel(lines.get(index), labelMatcher.end(), index, usedValueLines);
			if (sameLineValue != null) {
				putLabValue(fields, usedValueLines, key, sameLineValue);
				return;
			}

			LabValue nearbyValue = preferPreviousValue
					? firstNonNull(
							findNearbyLabValue(lines, index, -1, maxLinesBefore, usedValueLines),
							findNearbyLabValue(lines, index, 1, maxLinesAfter, usedValueLines))
					: firstNonNull(
							findNearbyLabValue(lines, index, 1, maxLinesAfter, usedValueLines),
							findNearbyLabValue(lines, index, -1, maxLinesBefore, usedValueLines));
			if (nearbyValue != null) {
				putLabValue(fields, usedValueLines, key, nearbyValue);
				return;
			}
		}
	}

	private LabValue firstNonNull(LabValue first, LabValue second) {
		return first == null ? second : first;
	}

	private void putLabValue(Map<String, String> fields, Set<Integer> usedValueLines, String key, LabValue labValue) {
		fields.put(key, labValue.value());
		usedValueLines.add(labValue.lineIndex());
	}

	private LabValue valueAfterLabel(String line, int labelEndIndex, int lineIndex, Set<Integer> usedValueLines) {
		String suffix = line.substring(labelEndIndex);
		String value = extractReliableLabValue(suffix);
		return value == null ? null : new LabValue(value, lineIndex);
	}

	private LabValue findNearbyLabValue(
			List<String> lines,
			int labelIndex,
			int direction,
			int maxDistance,
			Set<Integer> usedValueLines) {
		for (int distance = 1; distance <= maxDistance; distance++) {
			int valueIndex = labelIndex + (direction * distance);
			if (valueIndex < 0 || valueIndex >= lines.size() || usedValueLines.contains(valueIndex)) {
				continue;
			}
			String value = extractReliableLabValue(lines.get(valueIndex));
			if (value != null) {
				return new LabValue(value, valueIndex);
			}
		}
		return null;
	}

	private String extractReliableLabValue(String text) {
		String normalized = text == null ? "" : text.trim();
		if (normalized.isBlank()
				|| Pattern.compile("\\d+(?:\\.\\d+)?\\s*[-–]\\s*\\d+(?:\\.\\d+)?").matcher(normalized).find()
				|| Pattern.compile("\\b(?:low|high|reference|range|value|result|blood|count|indices)\\b", Pattern.CASE_INSENSITIVE).matcher(normalized).find()) {
			return null;
		}
		Matcher matcher = Pattern.compile("\\b([0-9]{1,7}(?:\\.[0-9]+)?\\s*(?:%|g/dl|gm/dl|mg/dl|/cumm|cells/u?l|x10\\^?\\d/?u?l|k/u?l|fl|pg)?)\\b", Pattern.CASE_INSENSITIVE)
				.matcher(normalized);
		if (!matcher.find()) {
			return null;
		}
		return matcher.group(1).trim();
	}

	private record LabValue(String value, int lineIndex) {
	}

	private Map<String, String> extractPrescriptionFields(String extractedText) {
		Map<String, String> fields = new LinkedHashMap<>();
		addFirstMatch(fields, "dosage", extractedText, "\\bdosage\\b\\s*[:\\-]?\\s*([A-Za-z0-9 .,/\\-]{2,80})");
		addFirstMatch(fields, "frequency", extractedText, "\\b((?:once|twice|thrice)\\s+(?:daily|a\\s+day)|every\\s+\\d+\\s+hours?)\\b");
		addFirstMatch(fields, "duration", extractedText, "\\b(?:for|duration)\\s*[:\\-]?\\s*(\\d+\\s*(?:days?|weeks?|months?))\\b");
		List<String> medicines = Pattern.compile("(?m)^\\s*(?:tab(?:let)?|cap(?:sule)?)\\.?\\s+([A-Za-z][A-Za-z0-9 .'-]{1,60})", Pattern.CASE_INSENSITIVE)
				.matcher(extractedText)
				.results()
				.map(result -> result.group(1).trim())
				.distinct()
				.toList();
		if (!medicines.isEmpty()) {
			fields.put("medicines", String.join(", ", medicines));
		}
		return fields;
	}

	private int evidenceScore(String text, String... regexes) {
		int score = 0;
		for (String regex : regexes) {
			if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find()) {
				score++;
			}
		}
		return score;
	}

	private void addFirstMatch(Map<String, String> fields, String key, String text, String regex) {
		Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
		if (matcher.find()) {
			fields.put(key, matcher.group(1).trim());
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
