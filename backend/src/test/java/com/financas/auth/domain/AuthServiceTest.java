package com.financas.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.financas.auth.infra.JwtService;
import com.financas.shared.exceptions.UnauthorizedException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService service;

    private static final String INVALID_CREDENTIALS_MESSAGE = "Usuário ou senha inválidos.";

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new AuthService(repository, passwordEncoder, jwtService);
    }

    @Test
    void issuesTokenWhenCredentialsAreCorrect() {
        User user = new User("sindica", "hashed-password");
        when(repository.findByUsername("sindica")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "hashed-password")).thenReturn(true);
        when(jwtService.generate("sindica")).thenReturn("a-jwt-token");

        String token = service.login("sindica", "senha123");

        assertThat(token).isEqualTo("a-jwt-token");
    }

    @Test
    void rejectsLoginWhenUsernameDoesNotExist() {
        when(repository.findByUsername("desconhecida")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("desconhecida", "senha123"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    void rejectsLoginWhenPasswordIsIncorrect() {
        User user = new User("sindica", "hashed-password");
        when(repository.findByUsername("sindica")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-errada", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> service.login("sindica", "senha-errada"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage(INVALID_CREDENTIALS_MESSAGE);
    }
}
