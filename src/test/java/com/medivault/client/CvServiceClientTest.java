package com.medivault.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class CvServiceClientTest {

	private final CvServiceClient client = new CvServiceClient(
			WebClient.builder(),
			"",
			"https://api.ocr.space/parse/image",
			"test-key");

	@Test
	void detectsLabReportFromMultipleLabEvidenceAndExtractsOnlyPresentValues() {
		String text = """
				Complete Blood Test - Lab Report
				Reference Range
				Hemoglobin: 13.5 g/dL
				Glucose: 95 mg/dL
				Platelet Count: 250 x10^3/uL
				Report Date: 16/09/2026
				""";

		Map<String, String> fields = client.extractStructuredFields(text);

		assertEquals("LAB_REPORT", client.detectDocumentType(text));
		assertEquals("13.5 g/dL", fields.get("hemoglobin"));
		assertEquals("95 mg/dL", fields.get("glucose"));
		assertEquals("250 x10^3/uL", fields.get("platelet_count"));
		assertEquals("16/09/2026", fields.get("reportDate"));
	}

	@Test
	void extractsFieldsFromCbcReportWithOcrSpellingAndLayoutErrors() {
		String text = """
				Complete Blood Count (CBC)

				Result
				Blood
				Reference Value
				12.5
				Low 13.0-17.0

				Investigation
				HEMO LOMIN
				Hemaglobin (Hb)
				HER COLST
				Total RBC coust

				BLOOD INDICES
				Packed Cell Volume (PCV)
				Mean Corpusculse Volume (MCW)
				MCHS
				ROW

				27.2
				32.8
				136

				PLATELET COUNT
				Poster coun
				152000

				DIFFERENTIAL WBC COUNT
				Lympliocytes
				brogy
				Monorses
				Basophils
				""";

		Map<String, String> fields = client.extractStructuredFields(text);

		assertEquals("LAB_REPORT", client.detectDocumentType(text));
		assertEquals("12.5", fields.get("hemoglobin"));
		assertEquals("27.2", fields.get("pcv"));
		assertEquals("32.8", fields.get("mcv"));
		assertEquals("152000", fields.get("platelet_count"));
		assertTrue(!fields.containsKey("rbc"));
		assertTrue(!fields.containsKey("wbc"));
	}

	@Test
	void detectsPrescriptionOnlyWhenMultiplePrescriptionSignalsExist() {
		String text = """
				Prescription
				Rx
				Tab Amoxicillin 500 mg
				Dosage: 1 tablet twice daily
				Duration: 5 days
				""";

		Map<String, String> fields = client.extractStructuredFields(text);

		assertEquals("PRESCRIPTION", client.detectDocumentType(text));
		assertEquals("1 tablet twice daily", fields.get("dosage"));
		assertEquals("twice daily", fields.get("frequency"));
		assertEquals("5 days", fields.get("duration"));
		assertEquals("Amoxicillin 500 mg", fields.get("medicines"));
	}

	@Test
	void returnsUnknownForArchitectureDiagramAndExtractsNoFields() {
		String text = """
				END-TO-END DIGITAL HEALTH RECORD WORKFLOW
				React Frontend
				Spring Boot Backend
				MongoDB
				Computer Vision
				Modiqo / Rote
				Scan -> Understand -> Record
				""";

		assertEquals("UNKNOWN", client.detectDocumentType(text));
		assertTrue(client.extractStructuredFields(text).isEmpty());
	}

	@Test
	void singleGenericPrescriptionWordIsInsufficient() {
		String text = "Prescription workflow architecture for Spring Boot OCR scanner";

		assertEquals("UNKNOWN", client.detectDocumentType(text));
		assertTrue(client.extractStructuredFields(text).isEmpty());
	}
}
