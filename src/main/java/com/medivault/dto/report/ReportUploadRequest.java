package com.medivault.dto.report;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ReportUploadRequest(@NotNull MultipartFile file) {
}
