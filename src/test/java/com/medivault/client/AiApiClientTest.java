package com.medivault.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.medivault.dto.ai.AiChatRequest;
import com.medivault.dto.ai.AiChatResponse;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class AiApiClientTest {

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void chatCallsGeminiAndReturnsExistingResponseShape() throws IOException {
		AtomicReference<String> requestBody = new AtomicReference<>();
		String baseUrl = startGeminiMock("""
				{"candidates":[{"content":{"parts":[{"text":"Hemoglobin is the protein "},{"text":"in red blood cells that carries oxygen."}]}}]}
				""", requestBody);
		AiApiClient client = new AiApiClient(WebClient.builder(), baseUrl, "fake-key", "gemini-test");

		AiChatResponse response = client.chat(new AiChatRequest("What is hemoglobin?", "conversation-1"));

		assertEquals("conversation-1", response.conversationId());
		assertEquals("Hemoglobin is the protein in red blood cells that carries oxygen.", response.reply());
		assertTrue(requestBody.get().contains("MediVault"));
		assertTrue(requestBody.get().contains("Do not diagnose diseases"));
		assertTrue(requestBody.get().contains("What is hemoglobin?"));
	}

	@Test
	void reportSummaryUsesMockedGeminiOutput() throws IOException {
		String baseUrl = startGeminiMock("""
				{"candidates":[{"content":{"parts":[{"text":"Report Type: Complete Blood Count\\n\\nKey Results:\\n- Hemoglobin: 12.5\\n\\nSummary:\\nCBC values supplied by OCR are summarized without diagnosis."}]}}]}
				""", new AtomicReference<>());
		AiApiClient client = new AiApiClient(WebClient.builder(), baseUrl, "fake-key", "gemini-test");

		String summary = client.summarizeReport(
				"LAB_REPORT",
				Map.of("hemoglobin", "12.5", "platelet_count", "152000"),
				"Complete Blood Count Hemoglobin 12.5 Platelet Count 152000");

		assertTrue(summary.contains("Report Type: Complete Blood Count"));
		assertTrue(summary.contains("Hemoglobin: 12.5"));
		assertTrue(summary.contains("without diagnosis"));
	}

	@Test
	void aiNotConfiguredFallsBackWithoutCallingGemini() {
		AiApiClient client = new AiApiClient(WebClient.builder(), "", "", "gemini-test");

		AiChatResponse chat = client.chat(new AiChatRequest("Explain this report", "conversation-2"));
		String summary = client.summarizeReport("LAB_REPORT", Map.of("hemoglobin", "12.5"), "Hemoglobin 12.5");

		assertEquals("conversation-2", chat.conversationId());
		assertEquals("AI service is not configured yet.", chat.reply());
		assertTrue(summary.contains("AI service is not configured yet."));
		assertTrue(summary.contains("hemoglobin: 12.5"));
	}

	private String startGeminiMock(String responseBody, AtomicReference<String> requestBody) throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			try (InputStream inputStream = exchange.getRequestBody()) {
				requestBody.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
			}
			byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(response);
			}
		});
		server.start();
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}
}
