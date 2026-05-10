package com.taskflow.service;

import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RefreshTokenRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.AuthResponse;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
import com.taskflow.exception.BusinessException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.repository.RoleRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_USER"));

        User user = User.builder()
            .username(request.username())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .roles(Set.of(userRole))
            .build();

        user = userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return AuthResponse.of(accessToken, refreshToken, user.getId(), user.getUsername(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            throw new BusinessException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.username()));

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return AuthResponse.of(accessToken, refreshToken, user.getId(), user.getUsername(), user.getEmail());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!jwtUtil.isTokenValid(token) || !jwtUtil.isRefreshToken(token)) {
            throw new BusinessException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return AuthResponse.of(accessToken, newRefreshToken, user.getId(), user.getUsername(), user.getEmail());
    }
}
