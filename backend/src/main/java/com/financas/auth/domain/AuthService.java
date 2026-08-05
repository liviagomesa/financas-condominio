package com.financas.auth.domain;

import com.financas.auth.infra.JwtService;
import com.financas.shared.exceptions.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String login(String username, String password) {
        User user = repository
                .findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Usuário ou senha inválidos."));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Usuário ou senha inválidos.");
        }
        return jwtService.generate(user.getUsername());
    }
}
