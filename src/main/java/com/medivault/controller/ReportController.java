package com.medivault.controller;

import com.medivault.dto.report.ReportResponse;
import com.medivault.dto.report.ReportUpdateRequest;
import com.medivault.service.ReportService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReportResponse upload(Principal principal, @RequestPart("file") MultipartFile file) {
		return reportService.upload(principal.getName(), file);
	}

	@GetMapping("/my")
	public List<ReportResponse> myReports(Principal principal) {
		return reportService.listMyReports(principal.getName());
	}

	@GetMapping("/{reportId}")
	public ReportResponse get(Principal principal, @PathVariable String reportId) {
		return reportService.getAccessibleReport(principal.getName(), reportId);
	}

	@PutMapping("/{reportId}")
	public ReportResponse update(
			Principal principal,
			@PathVariable String reportId,
			@Valid @RequestBody ReportUpdateRequest request) {
		return reportService.update(principal.getName(), reportId, request);
	}
}
