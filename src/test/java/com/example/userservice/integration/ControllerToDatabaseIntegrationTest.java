package com.example.userservice.integration;

import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cache.type=none")
@AutoConfigureMockMvc
@Testcontainers
class ControllerToDatabaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCompleteUserFlowFromControllerToDatabase() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan@example.com", "Ivanov")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ivan@example.com"))
                .andExpect(jsonPath("$.active").value(true));

        User savedUser = userRepository.findByEmailUsingJpql("ivan@example.com")
                .orElseThrow();
        Long userId = savedUser.getId();

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Ivan"));

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan.new@example.com", "Petrov")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Petrov"));

        assertThat(userRepository.findById(userId).orElseThrow().getSurname())
                .isEqualTo("Petrov");

        mockMvc.perform(patch("/api/v1/users/{id}/active", userId)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    void shouldCompletePaymentCardFlowFromControllerToDatabase() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("card.owner@example.com", "Owner")))
                .andExpect(status().isCreated());

        Long userId = userRepository
                .findByEmailUsingJpql("card.owner@example.com")
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("IVAN OWNER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.holder").value("IVAN OWNER"));

        PaymentCard savedCard = paymentCardRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();
        Long cardId = savedCard.getId();

        mockMvc.perform(get("/api/v1/cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId));

        mockMvc.perform(put("/api/v1/cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("PETR OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("PETR OWNER"));

        mockMvc.perform(get("/api/v1/users/{userId}/cards", userId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(patch("/api/v1/cards/{id}/active", cardId)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/v1/cards/{id}", cardId))
                .andExpect(status().isNoContent());

        assertThat(paymentCardRepository.findById(cardId)).isEmpty();
    }

    private String userJson(String email, String surname) {
        return """
                {
                  "name": "Ivan",
                  "surname": "%s",
                  "birthDate": "2000-05-15",
                  "email": "%s"
                }
                """.formatted(surname, email);
    }

    private String cardJson(String holder) {
        return """
                {
                  "number": "4111111111111111",
                  "holder": "%s",
                  "expirationDate": "2099-12-31"
                }
                """.formatted(holder);
    }
}
