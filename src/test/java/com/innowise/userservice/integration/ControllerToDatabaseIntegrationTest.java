package com.innowise.userservice.integration;

import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.support.PostgresIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class ControllerToDatabaseIntegrationTest extends PostgresIntegrationTest {

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
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan@example.com", "Ivanov")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ivan@example.com"))
                .andExpect(jsonPath("$.active").value(true));

        User savedUser = userRepository.findAll().getFirst();
        Long userId = savedUser.getId();

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Ivan"));

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("ivan.new@example.com", "Petrov")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surname").value("Petrov"));

        assertThat(userRepository.findById(userId).orElseThrow().getSurname())
                .isEqualTo("Petrov");

        mockMvc.perform(patch("/api/users/{id}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    void shouldCompletePaymentCardFlowFromControllerToDatabase() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson("card.owner@example.com", "Owner")))
                .andExpect(status().isCreated());

        Long userId = userRepository.findAll().getFirst()
                .getId();

        mockMvc.perform(post("/api/users/{userId}/cards", userId)
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

        mockMvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId));

        mockMvc.perform(put("/api/cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("PETR OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("PETR OWNER"));

        mockMvc.perform(get("/api/users/{userId}/cards", userId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(patch("/api/cards/{id}/status", cardId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/cards/{id}", cardId))
                .andExpect(status().isNoContent());

        assertThat(paymentCardRepository.findById(cardId)).isEmpty();
    }

    @Test
    void filtersApplyBeforePagination() throws Exception {
        for (String surname : new String[]{"Petrov", "Petrenko", "Ivanov"}) {
            mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                    .content(userJson(surname + "@example.com", surname))).andExpect(status().isCreated());
        }
        mockMvc.perform(get("/api/users").param("name", " IV ").param("surname", "pET")
                        .param("size", "1").param("page", "1").param("sort", "surname,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].surname").value("Petrov"));
        mockMvc.perform(get("/api/users").param("name", "nobody").param("surname", "Pet"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void enforcesActiveLimitOnCreateAndReactivationAndCascadesOnDelete() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content(userJson("limit@example.com", "Owner"))).andExpect(status().isCreated());
        long owner = userRepository.findAll().getFirst().getId();
        String collection = "/api/users/" + owner + "/cards";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(collection).contentType(MediaType.APPLICATION_JSON)
                    .content(cardJson("Owner " + i))).andExpect(status().isCreated());
        }
        mockMvc.perform(post(collection).contentType(MediaType.APPLICATION_JSON).content(cardJson("Sixth")))
                .andExpect(status().isConflict());
        long card = paymentCardRepository.findAll().getFirst().getId();
        String statusUrl = "/api/cards/" + card + "/status";
        mockMvc.perform(patch(statusUrl).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(statusUrl).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(collection).contentType(MediaType.APPLICATION_JSON).content(cardJson("Replacement")))
                .andExpect(status().isCreated());
        mockMvc.perform(patch(statusUrl).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/users/{id}", owner)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/cards/{id}", card)).andExpect(status().isNotFound());
        assertThat(paymentCardRepository.count()).isZero();
    }

    @Test
    void returnsClientErrorsWithTheirHttpStatuses() throws Exception {
        mockMvc.perform(patch("/api/users/1/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/users/1/status")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/api/missing")).andExpect(status().isNotFound());
    }

    @Test
    void twoConcurrentCreatesCannotBothTakeTheFifthSlot() throws Exception {
        User owner = userRepository.saveAndFlush(new User("Ivan", "Test", LocalDate.of(2000, 1, 1), "race@example.com"));
        String collection = "/api/users/" + owner.getId() + "/cards";
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post(collection).contentType(MediaType.APPLICATION_JSON).content(cardJson("Holder")))
                    .andExpect(status().isCreated());
        }
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Integer> create = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test did not start");
            return mockMvc.perform(post(collection).with(user("admin").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON).content(cardJson("Fifth")))
                    .andReturn().getResponse().getStatus();
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(create);
            var second = pool.submit(create);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(paymentCardRepository.countByUser_IdAndActiveTrue(owner.getId())).isEqualTo(5);
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
