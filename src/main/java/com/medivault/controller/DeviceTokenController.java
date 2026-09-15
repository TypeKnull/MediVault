package com.medivault.controller;

import com.medivault.dto.device.DeviceTokenRequestDto;
import com.medivault.dto.device.DeviceTokenResponseDto;
import com.medivault.service.DeviceTokenService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {

	private final DeviceTokenService deviceTokenService;

	public DeviceTokenController(DeviceTokenService deviceTokenService) {
		this.deviceTokenService = deviceTokenService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DeviceTokenResponseDto register(Principal principal, @Valid @RequestBody DeviceTokenRequestDto request) {
		return deviceTokenService.register(principal.getName(), request);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unregister(Principal principal, @Valid @RequestBody DeviceTokenRequestDto request) {
		deviceTokenService.unregister(principal.getName(), request);
	}
}
