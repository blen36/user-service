package com.example.userservice.integration;

import com.example.userservice.entity.*;
import com.example.userservice.repository.*;
import com.example.userservice.support.PostgresIntegrationTest;
import com.example.userservice.support.TestJwtKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import java.time.*;
import java.util.Objects;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.cache.type=redis", "spring.cache.redis.time-to-live=10m"})
@AutoConfigureMockMvc
class JwtSecurityIntegrationTest extends PostgresIntegrationTest {
    private static final GenericContainer<?> REDIS = startRedis();
    private static GenericContainer<?> startRedis() {
        var redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        redis.start();
        return redis;
    }
    @DynamicPropertySource static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PaymentCardRepository cards;
    @Autowired CacheManager caches;
    User first;
    User second;
    PaymentCard ownCard;
    PaymentCard otherCard;
    String userToken;
    String adminToken;

    @BeforeEach void setUp() {
        Objects.requireNonNull(caches.getCache("users")).clear();
        cards.deleteAll();
        users.deleteAll();
        first = users.saveAndFlush(new User("First", "Tester", LocalDate.of(2000, 5, 15), "first@example.test"));
        second = users.saveAndFlush(new User("Second", "Tester", LocalDate.of(2000, 5, 15), "second@example.test"));
        ownCard = saveCard(first, "4111111111111111");
        otherCard = saveCard(second, "5555555555554444");
        userToken = TestJwtKeys.access(first.getId(), "USER");
        adminToken = TestJwtKeys.access(first.getId(), "ADMIN");
    }

    @Test void pingIsPublic() throws Exception {
        mvc.perform(get("/api/ping")).andExpect(status().isOk());
    }
    @Test void anonymousRequestGetsJson401() throws Exception {
        mvc.perform(get(userUrl(first))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
    }
    @Test void ownProfileIsAccessible() throws Exception {
        mvc.perform(get(userUrl(first)).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(first.getId()));
    }
    @Test void otherProfileIsForbidden() throws Exception {
        mvc.perform(get(userUrl(second)).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }
    @Test void userCannotListAllProfiles() throws Exception {
        mvc.perform(get("/api/v1/users").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }
    @Test void adminCanListProfiles() throws Exception {
        mvc.perform(get("/api/v1/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }
    @Test void adminCanReadAnotherProfile() throws Exception {
        mvc.perform(get(userUrl(second)).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }
    @Test void ownCardIsAccessible() throws Exception {
        mvc.perform(get(cardUrl(ownCard)).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk());
    }
    @Test void otherCardIsForbidden() throws Exception {
        mvc.perform(get(cardUrl(otherCard)).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }
    @Test void userCannotListAllCards() throws Exception {
        mvc.perform(get("/api/v1/cards").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }
    @Test void ownCardListIsAccessible() throws Exception {
        mvc.perform(get(userUrl(first) + "/cards").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk());
    }
    @Test void otherCardListIsForbidden() throws Exception {
        mvc.perform(get(userUrl(second) + "/cards").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }
    @Test void userCannotCreateProfiles() throws Exception {
        mvc.perform(post("/api/v1/users").header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON).content(profileJson("third@example.test")))
                .andExpect(status().isForbidden());
        assertThat(users.count()).isEqualTo(2);
    }
    @Test void userCanUpdateOwnProfile() throws Exception {
        mvc.perform(put(userUrl(first)).header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON).content(profileJson("changed@example.test")))
                .andExpect(status().isOk());
        assertThat(users.findById(first.getId()).orElseThrow().getEmail()).isEqualTo("changed@example.test");
    }
    @Test void userCannotUpdateOtherProfile() throws Exception {
        mvc.perform(put(userUrl(second)).header("Authorization", bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON).content(profileJson("changed@example.test")))
                .andExpect(status().isForbidden());
        assertThat(users.findById(second.getId()).orElseThrow().getEmail()).isEqualTo("second@example.test");
    }
    @Test void userCannotDeleteOtherCard() throws Exception {
        mvc.perform(delete(cardUrl(otherCard)).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        assertThat(cards.existsById(otherCard.getId())).isTrue();
    }
    @Test void userCannotDeactivateOwnProfile() throws Exception {
        mvc.perform(patch(userUrl(first) + "/active").param("active", "false")
                .header("Authorization", bearer(userToken))).andExpect(status().isForbidden());
        assertThat(users.findById(first.getId()).orElseThrow().isActive()).isTrue();
    }
    @Test void userCannotDeactivateOtherCard() throws Exception {
        mvc.perform(patch(cardUrl(otherCard) + "/active").param("active", "false")
                .header("Authorization", bearer(userToken))).andExpect(status().isForbidden());
        assertThat(cards.findById(otherCard.getId()).orElseThrow().isActive()).isTrue();
    }
    @Test void warmRedisCacheDoesNotLetInactiveUserThrough() throws Exception {
        assertThat(caches).isInstanceOf(RedisCacheManager.class);
        mvc.perform(get(userUrl(first)).header("Authorization", bearer(userToken))).andExpect(status().isOk());
        assertThat(Objects.requireNonNull(caches.getCache("users")).get(first.getId())).isNotNull();
        // Simulate a DB change without cache eviction: authorization must still read fresh state.
        first.setActive(false);
        users.saveAndFlush(first);
        mvc.perform(get(userUrl(first)).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get(cardUrl(ownCard)).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }
    @Test void refreshTokenCannotAuthorizeUserService() throws Exception {
        assertInvalid(TestJwtKeys.token(first.getId(), "USER", "refresh", "authentication-service",
                "java-advanced-api", Instant.now().plusSeconds(300)));
    }
    @Test void expiredTokenIsRejected() throws Exception {
        assertInvalid(TestJwtKeys.token(first.getId(), "USER", "access", "authentication-service",
                "java-advanced-api", Instant.now().minusSeconds(120)));
    }
    @Test void wrongIssuerIsRejected() throws Exception {
        assertInvalid(TestJwtKeys.token(first.getId(), "USER", "access", "other",
                "java-advanced-api", Instant.now().plusSeconds(300)));
    }
    @Test void wrongAudienceIsRejected() throws Exception {
        assertInvalid(TestJwtKeys.token(first.getId(), "USER", "access", "authentication-service",
                "other-api", Instant.now().plusSeconds(300)));
    }
    @Test void tamperedSignatureIsRejected() throws Exception {
        int pos = userToken.lastIndexOf('.') + 1;
        char replacement = userToken.charAt(pos) == 'A' ? 'B' : 'A';
        assertInvalid(userToken.substring(0, pos) + replacement + userToken.substring(pos + 1));
    }
    @Test void malformedTokenIsRejected() throws Exception { assertInvalid("not-a-jwt"); }

    private void assertInvalid(String token) throws Exception {
        mvc.perform(get(userUrl(first)).header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }
    private PaymentCard saveCard(User user, String number) {
        PaymentCard card = new PaymentCard(number, "Test Holder", LocalDate.of(2099, 12, 31));
        card.setUser(user);
        return cards.saveAndFlush(card);
    }
    private String bearer(String token) { return "Bearer " + token; }
    private String userUrl(User user) { return "/api/v1/users/" + user.getId(); }
    private String cardUrl(PaymentCard card) { return "/api/v1/cards/" + card.getId(); }
    private String profileJson(String email) {
        return "{\"name\":\"Updated\",\"surname\":\"Tester\",\"birthDate\":\"2000-05-15\",\"email\":\""
                + email + "\"}";
    }
}
