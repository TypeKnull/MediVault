package com.medivault.service;

import com.medivault.client.CvServiceClient;
import com.medivault.dto.cv.CvExtractionResponse;
import com.medivault.dto.report.ReportResponse;
import com.medivault.dto.report.ReportUpdateRequest;
import com.medivault.exception.PermissionDeniedException;
import com.medivault.exception.ResourceNotFoundException;
import com.medivault.model.Report;
import com.medivault.model.User;
import com.medivault.model.enums.ReportStatus;
import com.medivault.model.enums.Role;
import com.medivault.repository.ReportRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportService {

	private final ReportRepository reportRepository;
	private final AuthService authService;
	private final FileStorageService fileStorageService;
	private final CvServiceClient cvServiceClient;
	private final AiSummaryService aiSummaryService;
	private final PermissionService permissionService;

	public ReportService(
			ReportRepository reportRepository,
			AuthService authService,
			FileStorageService fileStorageService,
			CvServiceClient cvServiceClient,
			AiSummaryService aiSummaryService,
			PermissionService permissionService) {
		this.reportRepository = reportRepository;
		this.authService = authService;
		this.fileStorageService = fileStorageService;
		this.cvServiceClient = cvServiceClient;
		this.aiSummaryService = aiSummaryService;
		this.permissionService = permissionService;
	}

	public ReportResponse upload(String patientEmail, MultipartFile file) {
		User patient = requireRole(authService.findByEmail(patientEmail), Role.PATIENT);
		FileStorageService.StoredFile storedFile = fileStorageService.store(file);

		Report report = Report.builder()
				.patientId(patient.getId())
				.originalFileName(storedFile.originalFileName())
				.contentType(storedFile.contentType())
				.fileUrl(storedFile.fileUrl())
				.status(ReportStatus.PROCESSING)
				.build();
		report = reportRepository.save(report);

		try {
			CvExtractionResponse extraction = cvServiceClient.extract(storedFile.fileUrl());
			report.setExtractedText(extraction == null ? "" : extraction.extractedText());
			report.setDocumentType(extraction == null ? "UNKNOWN" : extraction.documentType());
			report.setFields(extraction == null ? null : extraction.fields());
			report.setAiSummary(aiSummaryService.summarize(report.getExtractedText()));
			report.setStatus(ReportStatus.PROCESSED);
		} catch (RuntimeException exception) {
			report.setStatus(ReportStatus.FAILED);
			report.setAiSummary("Report processing failed: " + exception.getMessage());
		}

		return toResponse(reportRepository.save(report));
	}

	public List<ReportResponse> listMyReports(String email) {
		User patient = requireRole(authService.findByEmail(email), Role.PATIENT);
		return reportRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
				.map(this::toResponse)
				.toList();
	}

	public ReportResponse getAccessibleReport(String email, String reportId) {
		User user = authService.findByEmail(email);
		Report report = findReport(reportId);
		ensureReportAccess(user, report);
		return toResponse(report);
	}

	public ReportResponse update(String email, String reportId, ReportUpdateRequest request) {
		User patient = requireRole(authService.findByEmail(email), Role.PATIENT);
		Report report = findReport(reportId);
		if (!patient.getId().equals(report.getPatientId())) {
			throw new PermissionDeniedException("You can only update your own reports");
		}
		if (request.extractedText() != null) {
			report.setExtractedText(request.extractedText());
		}
		if (request.documentType() != null) {
			report.setDocumentType(request.documentType());
		}
		if (request.fields() != null) {
			report.setFields(request.fields());
		}
		if (request.aiSummary() != null) {
			report.setAiSummary(request.aiSummary());
		}
		report.setStatus(ReportStatus.PROCESSED);
		return toResponse(reportRepository.save(report));
	}

	private void ensureReportAccess(User user, Report report) {
		if (user.getRole() == Role.PATIENT && user.getId().equals(report.getPatientId())) {
			return;
		}
		if (user.getRole() == Role.DOCTOR && permissionService.hasApprovedAccess(user.getId(), report.getPatientId())) {
			return;
		}
		throw new PermissionDeniedException("You do not have access to this report");
	}

	private Report findReport(String reportId) {
		return reportRepository.findById(reportId)
				.orElseThrow(() -> new ResourceNotFoundException("Report not found"));
	}

	private User requireRole(User user, Role role) {
		if (user.getRole() != role) {
			throw new PermissionDeniedException("Required role: " + role);
		}
		return user;
	}

	private ReportResponse toResponse(Report report) {
		return new ReportResponse(
				report.getId(),
				report.getPatientId(),
				report.getOriginalFileName(),
				report.getContentType(),
				report.getFileUrl(),
				report.getExtractedText(),
				report.getDocumentType(),
				report.getFields(),
				report.getAiSummary(),
				report.getStatus(),
				report.getCreatedAt(),
				report.getUpdatedAt());
	}
}
