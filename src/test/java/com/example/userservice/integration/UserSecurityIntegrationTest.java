package com.example.userservice.integration;

import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInternalRequestWithoutRegistrationKey() throws Exception {
        mockMvc.perform(post("/internal/users"))
                .andExpect(status().isUnauthorized());
    }

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
                        .claim("userId", owner.getId())
                        .claim("role", "USER")
                )
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));

        mockMvc.perform(get("/api/v1/users/{id}", owner.getId())
                        .with(authentication))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/{id}", other.getId())
                        .with(authentication))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users")
                        .with(authentication))
                .andExpect(status().isForbidden());
    }
}