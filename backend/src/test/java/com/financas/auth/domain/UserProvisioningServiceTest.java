package com.financas.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    private static final String VALID_SECRET = "a-secret-with-at-least-32-characters!!";

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsUserWhenUsernameDoesNotExist() {
        MockEnvironment environment = environmentWith("sindica", "senha123", VALID_SECRET);
        when(repository.findByUsername("sindica")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha123")).thenReturn("hashed-password");

        new UserProvisioningService(repository, passwordEncoder, environment).run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("sindica");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void updatesPasswordHashWhenUserAlreadyExists() {
        MockEnvironment environment = environmentWith("sindica", "nova-senha", VALID_SECRET);
        User existing = new User("sindica", "old-hash");
        when(repository.findByUsername("sindica")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("nova-senha")).thenReturn("new-hash");

        new UserProvisioningService(repository, passwordEncoder, environment).run(null);

        verify(repository).save(existing);
        assertThat(existing.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void throwsWhenAdminUsernameIsMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("ADMIN_PASSWORD", "senha123");
        environment.setProperty("JWT_SECRET", VALID_SECRET);

        assertThatThrownBy(() ->
                        new UserProvisioningService(repository, passwordEncoder, environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_USERNAME");
        verify(repository, never()).save(any());
    }

    @Test
    void throwsWhenAdminPasswordIsMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("ADMIN_USERNAME", "sindica");
        environment.setProperty("JWT_SECRET", VALID_SECRET);

        assertThatThrownBy(() ->
                        new UserProvisioningService(repository, passwordEncoder, environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
        verify(repository, never()).save(any());
    }

    @Test
    void throwsWhenJwtSecretIsMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("ADMIN_USERNAME", "sindica");
        environment.setProperty("ADMIN_PASSWORD", "senha123");

        assertThatThrownBy(() ->
                        new UserProvisioningService(repository, passwordEncoder, environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        verify(repository, never()).save(any());
    }

    @Test
    void throwsWhenJwtSecretIsShorterThanMinimumLength() {
        MockEnvironment environment = environmentWith("sindica", "senha123", "too-short-secret");

        assertThatThrownBy(() ->
                        new UserProvisioningService(repository, passwordEncoder, environment).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        verify(repository, never()).save(any());
    }

    private MockEnvironment environmentWith(String username, String password, String jwtSecret) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("ADMIN_USERNAME", username);
        environment.setProperty("ADMIN_PASSWORD", password);
        environment.setProperty("JWT_SECRET", jwtSecret);
        return environment;
    }
}
