package com.innowise.userservice.integration;

import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.support.PostgresIntegrationTest;
import com.innowise.userservice.support.TestSecurityProperties;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserSecurityIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository users;

    @Test
    void rejectsRequestsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInternalRequestWithoutRegistrationKey() throws Exception {
        mockMvc.perform(post("/internal/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationKeyCanOnlyCreateAndDeleteProfiles() throws Exception {
        String key = TestSecurityProperties.REGISTRATION_KEY;
        String email = UUID.randomUUID() + "@example.com";
        String profile = """
                {"name":"Jane","surname":"Doe","birthDate":"2000-01-01","email":"%s"}
                """.formatted(email);
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(profile))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users").header("X-Registration-Key", "wrong")
                .contentType(MediaType.APPLICATION_JSON).content(profile)).andExpect(status().isUnauthorized());
        String body = mockMvc.perform(post("/api/users").header("X-Registration-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(profile)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(body).get("id").asLong();
        mockMvc.perform(get("/api/users").header("X-Registration-Key", key)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/users/{id}", id).header("X-Registration-Key", key)).andExpect(status().isNoContent());
    }

    @Autowired ObjectMapper mapper;

    @Test
    void userCanReadOwnProfileButNotAnotherProfileOrAllUsers()
            throws Exception {

        User owner = users.save(new User(
                "Owner",
                "Test",
                LocalDate.of(2000, 1, 1),
                UUID.randomUUID() + "@example.com"
        ));

        User other = users.save(new User(
                "Other",
                "Test",
                LocalDate.of(2000, 1, 1),
                UUID.randomUUID() + "@example.com"
        ));

        var authentication = jwt()
                .jwt(token -> token
                        .subject(owner.getId().toString())
                        .claim("user_id", owner.getId())
                        .claim("role", "USER")
                )
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));

        mockMvc.perform(get("/api/users/{id}", owner.getId())
                        .with(authentication))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/{id}", other.getId())
                        .with(authentication))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users")
                        .with(authentication))
                .andExpect(status().isForbidden());
    }
}
