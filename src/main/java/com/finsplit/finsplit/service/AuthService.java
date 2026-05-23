package com.finsplit.finsplit.service;

import com.finsplit.finsplit.dto.AuthDTOs.*;
import com.finsplit.finsplit.entity.User;
import com.finsplit.finsplit.exception.UserAlreadyExistsException;
import com.finsplit.finsplit.repository.UserRepository;
import com.finsplit.finsplit.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
        import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email()))
            throw new UserAlreadyExistsException("Email already in use: " + req.email());

        var user = User.builder()
                .name(req.name())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .build();
        userRepository.save(user);

        return new AuthResponse(jwtUtil.generateToken(user.getEmail()), user.getEmail(), user.getName());
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        var user = userRepository.findByEmail(req.email()).orElseThrow();
        return new AuthResponse(jwtUtil.generateToken(user.getEmail()), user.getEmail(), user.getName());
    }
}
