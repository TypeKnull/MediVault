package com.medivault.service;

import com.medivault.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

	private final Path uploadDirectory;

	public FileStorageService(@Value("${medivault.storage.upload-dir}") String uploadDirectory) {
		this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
	}

	@PostConstruct
	void init() throws IOException {
		Files.createDirectories(uploadDirectory);
	}

	public StoredFile store(MultipartFile file) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Uploaded file is empty");
		}
		String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "report" : file.getOriginalFilename());
		String storedName = UUID.randomUUID() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
		Path destination = uploadDirectory.resolve(storedName).normalize();
		if (!destination.startsWith(uploadDirectory)) {
			throw new IllegalArgumentException("Invalid file name");
		}

		try (InputStream inputStream = file.getInputStream()) {
			Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
			return new StoredFile(originalName, file.getContentType(), destination.toString());
		} catch (IOException exception) {
			throw new IllegalStateException("Unable to store uploaded file", exception);
		}
	}

	public Path load(String fileUrl) {
		Path path = Path.of(fileUrl).toAbsolutePath().normalize();
		if (!Files.exists(path)) {
			throw new ResourceNotFoundException("Stored file not found");
		}
		return path;
	}

	public record StoredFile(String originalFileName, String contentType, String fileUrl) {
	}
}
