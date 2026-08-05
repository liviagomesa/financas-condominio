package com.financas.auth;

import static org.hamcrest.Matchers.is;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFilterIT {

    @Autowired
    private MockMvc mockMvc;

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Test
    void rejectsRequestToProtectedRouteWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/parties"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", is("Não autenticado.")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", is(401)));
    }

    @Test
    void allowsRequestToProtectedRouteWithValidToken() throws Exception {
        String token = login();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/parties")
                        .header("Authorization", "Bearer " + token))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void allowsLoginWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sindica\",\"password\":\"senha-de-teste\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void allowsLoginEvenWhenAValidTokenIsAlreadyPresent() throws Exception {
        String token = login();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sindica\",\"password\":\"senha-de-teste\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void rejectsRequestToProtectedRouteWithMalformedToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/parties")
                        .header("Authorization", "Bearer token-malformado-invalido"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", is("Não autenticado.")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", is(401)));
    }

    @Test
    void rejectsRequestToProtectedRouteWithExpiredToken() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date(System.currentTimeMillis() - 2000);
        Date expiration = new Date(System.currentTimeMillis() - 1000);
        String expiredToken = Jwts.builder()
                .subject("sindica")
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/parties")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", is("Não autenticado.")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status", is(401)));
    }

    @Test
    void rejectsRequestToNonExistentRouteWithoutTokenAsUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/rota-inexistente"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void reportsNotFoundForNonExistentRouteWithValidToken() throws Exception {
        String token = login();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/rota-inexistente")
                        .header("Authorization", "Bearer " + token))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    private String login() throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sindica\",\"password\":\"senha-de-teste\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.token");
    }
}
