package com.medivault.controller;

import com.medivault.dto.ai.AiChatRequest;
import com.medivault.dto.ai.AiChatResponse;
import com.medivault.dto.ai.AiSummaryResponse;
import com.medivault.dto.report.ReportResponse;
import com.medivault.service.AiChatService;
import com.medivault.service.AiSummaryService;
import com.medivault.service.ReportService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

	private final AiChatService aiChatService;
	private final AiSummaryService aiSummaryService;
	private final ReportService reportService;

	public AiController(AiChatService aiChatService, AiSummaryService aiSummaryService, ReportService reportService) {
		this.aiChatService = aiChatService;
		this.aiSummaryService = aiSummaryService;
		this.reportService = reportService;
	}

	@PostMapping("/chat")
	public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
		return aiChatService.chat(request);
	}

	@GetMapping("/reports/{reportId}/summary")
	public AiSummaryResponse summary(Principal principal, @PathVariable String reportId) {
		ReportResponse report = reportService.getAccessibleReport(principal.getName(), reportId);
		return new AiSummaryResponse(report.id(), aiSummaryService.summarizeReport(report));
	}
}
