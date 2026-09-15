package com.medivault.security;

import com.medivault.model.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

	private final byte[] secret;
	private final long accessTokenExpirationMs;
	private final long refreshTokenExpirationMs;

	public JwtUtil(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
			@Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		this.accessTokenExpirationMs = accessTokenExpirationMs;
		this.refreshTokenExpirationMs = refreshTokenExpirationMs;
	}

	public String generateAccessToken(User user) {
		return generateToken(user, "access", accessTokenExpirationMs);
	}

	public String generateRefreshToken(User user) {
		return generateToken(user, "refresh", refreshTokenExpirationMs);
	}

	public JwtClaims validate(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				throw new IllegalArgumentException("Malformed token");
			}
			String signedContent = parts[0] + "." + parts[1];
			if (!constantTimeEquals(parts[2], sign(signedContent))) {
				throw new IllegalArgumentException("Invalid token signature");
			}
			Map<String, String> payload = parseJson(new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8));
			long expiresAt = Long.parseLong(payload.get("exp"));
			if (Instant.now().getEpochSecond() >= expiresAt) {
				throw new IllegalArgumentException("Expired token");
			}
			return new JwtClaims(
					payload.get("sub"),
					payload.get("userId"),
					payload.get("role"),
					payload.get("type"),
					expiresAt);
		} catch (Exception exception) {
			throw new IllegalArgumentException("Invalid JWT", exception);
		}
	}

	private String generateToken(User user, String type, long expirationMs) {
		try {
			Instant now = Instant.now();
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("sub", user.getEmail());
			payload.put("userId", user.getId());
			payload.put("role", user.getRole().name());
			payload.put("type", type);
			payload.put("iat", now.getEpochSecond());
			payload.put("exp", now.plusMillis(expirationMs).getEpochSecond());

			String encodedHeader = base64UrlEncode(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
			String encodedPayload = base64UrlEncode(toJson(payload).getBytes(StandardCharsets.UTF_8));
			String signedContent = encodedHeader + "." + encodedPayload;
			return signedContent + "." + sign(signedContent);
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to generate JWT", exception);
		}
	}

	private String sign(String value) throws Exception {
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
		return base64UrlEncode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
	}

	private String base64UrlEncode(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	private byte[] base64UrlDecode(String value) {
		return Base64.getUrlDecoder().decode(value);
	}

	private String toJson(Map<String, Object> values) {
		return values.entrySet().stream()
				.map(entry -> "\"" + escape(entry.getKey()) + "\":" + jsonValue(entry.getValue()))
				.collect(Collectors.joining(",", "{", "}"));
	}

	private String jsonValue(Object value) {
		if (value instanceof Number || value instanceof Boolean) {
			return value.toString();
		}
		return "\"" + escape(value == null ? "" : value.toString()) + "\"";
	}

	private Map<String, String> parseJson(String json) {
		Map<String, String> values = new LinkedHashMap<>();
		String body = json.trim();
		if (body.startsWith("{")) {
			body = body.substring(1);
		}
		if (body.endsWith("}")) {
			body = body.substring(0, body.length() - 1);
		}
		for (String entry : body.split(",")) {
			String[] pair = entry.split(":", 2);
			if (pair.length == 2) {
				values.put(unquote(pair[0].trim()), unquote(pair[1].trim()));
			}
		}
		return values;
	}

	private String unquote(String value) {
		String trimmed = value.trim();
		if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
			trimmed = trimmed.substring(1, trimmed.length() - 1);
		}
		return trimmed
				.replace("\\\"", "\"")
				.replace("\\\\", "\\");
	}

	private String escape(String value) {
		return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"");
	}

	private boolean constantTimeEquals(String left, String right) {
		if (left.length() != right.length()) {
			return false;
		}
		int result = 0;
		for (int index = 0; index < left.length(); index++) {
			result |= left.charAt(index) ^ right.charAt(index);
		}
		return result == 0;
	}

	public record JwtClaims(String subject, String userId, String role, String type, long expiresAt) {
	}
}
