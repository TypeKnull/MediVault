package com.medivault.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

	private final JwtUtil jwtUtil;
	private final CustomUserDetailsService userDetailsService;

	public WebSocketAuthInterceptor(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
		this.jwtUtil = jwtUtil;
		this.userDetailsService = userDetailsService;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String authHeader = accessor.getFirstNativeHeader("Authorization");
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				JwtUtil.JwtClaims claims = jwtUtil.validate(authHeader.substring(7));
				UserDetails userDetails = userDetailsService.loadUserByUsername(claims.subject());
				accessor.setUser(new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities()));
			}
		}
		return message;
	}
}
