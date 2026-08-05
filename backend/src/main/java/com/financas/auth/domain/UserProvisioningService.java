package com.financas.auth.domain;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserProvisioningService implements ApplicationRunner {

    private static final int MIN_JWT_SECRET_LENGTH = 32;

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public UserProvisioningService(
            UserRepository repository, PasswordEncoder passwordEncoder, Environment environment) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = requireNonBlank("ADMIN_USERNAME");
        String password = requireNonBlank("ADMIN_PASSWORD");
        String jwtSecret = requireNonBlank("JWT_SECRET");
        if (jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_JWT_SECRET_LENGTH + " characters long.");
        }

        String passwordHash = passwordEncoder.encode(password);
        User user = repository
                .findByUsername(username)
                .map(existing -> {
                    existing.setPasswordHash(passwordHash);
                    return existing;
                })
                .orElseGet(() -> new User(username, passwordHash));
        repository.save(user);
    }

    private String requireNonBlank(String propertyName) {
        String value = environment.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " environment variable must be set.");
        }
        return value;
    }
}
