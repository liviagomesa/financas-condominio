package com.financas.auth.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtServiceTest {

    private static final String SECRET = "a-secret-with-at-least-32-characters!!";

    @Test
    void generatesTokenWithSubjectAndSevenDayExpiration() {
        JwtService jwtService = newJwtService(SECRET);

        String token = jwtService.generate("sindica");

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("sindica");
        long validityMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(validityMillis).isEqualTo(java.time.Duration.ofDays(7).toMillis());
    }

    @Test
    void validatesValidToken() {
        JwtService jwtService = newJwtService(SECRET);
        String token = jwtService.generate("sindica");

        String username = jwtService.validate(token);

        assertThat(username).isEqualTo("sindica");
    }

    @Test
    void rejectsExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Date past = new Date(System.currentTimeMillis() - 1000);
        String expiredToken = Jwts.builder()
                .subject("sindica")
                .issuedAt(new Date(past.getTime() - 1000))
                .expiration(past)
                .signWith(key)
                .compact();

        JwtService jwtService = newJwtService(SECRET);

        assertThatThrownBy(() -> jwtService.validate(expiredToken)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsTokenWithInvalidSignature() {
        JwtService issuedWithAnotherSecret = newJwtService("a-different-secret-with-32-chars!!!");
        String token = issuedWithAnotherSecret.generate("sindica");

        JwtService jwtService = newJwtService(SECRET);

        assertThatThrownBy(() -> jwtService.validate(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenGeneratedByOneInstanceIsValidatedByAnotherWithTheSameSecret() {
        JwtService firstInstance = newJwtService(SECRET);
        String token = firstInstance.generate("sindica");

        JwtService secondInstance = newJwtService(SECRET);

        assertThat(secondInstance.validate(token)).isEqualTo("sindica");
    }

    private JwtService newJwtService(String secret) {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("JWT_SECRET", secret);
        return new JwtService(environment);
    }
}
