package com.tradeball.service;

import com.tradeball.domain.Role;
import com.tradeball.dto.AuthResponse;
import com.tradeball.dto.LoginRequest;
import com.tradeball.dto.RegisterRequest;
import com.tradeball.dto.UserResponse;
import com.tradeball.entity.UserEntity;
import com.tradeball.exception.ApiErrorCode;
import com.tradeball.exception.ApiException;
import com.tradeball.exception.ConflictException;
import com.tradeball.mapper.UserMapper;
import com.tradeball.repository.UserRepository;
import com.tradeball.security.JwtService;
import com.tradeball.security.SecurityUtils;
import com.tradeball.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already registered");
        }
        UserEntity user = new UserEntity();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setRole(Role.USER);
        UserEntity saved = userRepository.save(user);
        log.info("User registered id={}", saved.getId());
        String token = jwtService.generateToken(saved.getId(), saved.getEmail(), saved.getRole());
        return AuthResponse.bearer(token, userMapper.toResponse(saved));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ApiException(ApiErrorCode.AUTHENTICATION_ERROR, HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        log.info("User authenticated id={}", user.getId());
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return AuthResponse.bearer(token, userMapper.toResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        UserPrincipal principal = SecurityUtils.currentUser();
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "User not found"));
        return userMapper.toResponse(user);
    }
}
