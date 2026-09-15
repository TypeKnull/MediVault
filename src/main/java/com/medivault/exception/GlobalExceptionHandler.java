package com.medivault.exception;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(PermissionDeniedException.class)
	ResponseEntity<Map<String, Object>> handleForbidden(PermissionDeniedException exception) {
		return error(HttpStatus.FORBIDDEN, exception.getMessage());
	}

	@ExceptionHandler(UnauthorizedAccessException.class)
	ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedAccessException exception) {
		return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
						(first, ignored) -> first))
				.toString();
		return error(HttpStatus.BAD_REQUEST, message);
	}

	private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(Map.of(
				"timestamp", Instant.now(),
				"status", status.value(),
				"error", status.getReasonPhrase(),
				"message", message));
	}
}
