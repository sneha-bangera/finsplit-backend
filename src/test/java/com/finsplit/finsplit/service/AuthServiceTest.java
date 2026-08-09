package com.finsplit.finsplit.service;

import com.finsplit.finsplit.dto.AuthDTOs.*;
import com.finsplit.finsplit.entity.User;
import com.finsplit.finsplit.exception.UserAlreadyExistsException;
import com.finsplit.finsplit.repository.UserRepository;
import com.finsplit.finsplit.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository      userRepository;
    @Mock private PasswordEncoder     passwordEncoder;
    @Mock private JwtUtil             jwtUtil;
    @Mock private AuthenticationManager authManager;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .name("Rahul Shah")
                .email("rahul@example.com")
                .password("encoded_password")
                .build();
    }

    // ── Register tests ────────────────────────────────────

    @Test
    @DisplayName("Register: success — new user gets token back")
    void register_success() {
        when(userRepository.existsByEmail("rahul@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtUtil.generateToken("rahul@example.com")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(
                new RegisterRequest("Rahul Shah", "rahul@example.com", "secret123"));

        assertThat(response.token()).isEqualTo("mock.jwt.token");
        assertThat(response.email()).isEqualTo("rahul@example.com");
        assertThat(response.name()).isEqualTo("Rahul Shah");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    @DisplayName("Register: fails when email already exists")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("rahul@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register(
                        new RegisterRequest("Rahul Shah", "rahul@example.com", "secret123"))
        )
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("rahul@example.com");

        // Confirm user was never saved
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register: password is encoded — never stored in plaintext")
    void register_passwordIsEncoded() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.register(
                new RegisterRequest("Rahul Shah", "rahul@example.com", "secret123"));

        // Verify encoder was called with the raw password
        verify(passwordEncoder).encode("secret123");
        // Verify save was called (with encoded password inside)
        verify(userRepository).save(argThat(user ->
                !user.getPassword().equals("secret123") // must not be plaintext
        ));
    }

    // ── Login tests ───────────────────────────────────────

    @Test
    @DisplayName("Login: success — returns token for valid credentials")
    void login_success() {
        when(userRepository.findByEmail("rahul@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken("rahul@example.com"))
                .thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(
                new LoginRequest("rahul@example.com", "secret123"));

        assertThat(response.token()).isEqualTo("mock.jwt.token");
        assertThat(response.email()).isEqualTo("rahul@example.com");
    }

    @Test
    @DisplayName("Login: fails with wrong password")
    void login_wrongPassword_throwsBadCredentials() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("rahul@example.com", "wrongpass"))
        ).isInstanceOf(BadCredentialsException.class);
    }
}