package com.example.userservice.repository;

import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true"
})
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveUpdateAndDeleteUserWithCard() throws InterruptedException {
        // Arrange — подготавливаем объекты

        User user = new User();
        user.setName("Ivan");
        user.setSurname("Ivanov");
        user.setBirthDate(LocalDate.of(2000, 5, 15));
        user.setEmail(
                "integration-" + UUID.randomUUID() + "@example.com"
        );
        user.setActive(true);

        PaymentCard card = new PaymentCard();
        card.setNumber("4111111111111111");
        card.setHolder("IVAN IVANOV");
        card.setExpirationDate(LocalDate.of(2030, 12, 31));
        card.setActive(true);

        user.addPaymentCard(card);

        // Act — сохраняем только пользователя

        User savedUser = userRepository.saveAndFlush(user);

        Long userId = savedUser.getId();
        Long cardId = card.getId();

        // Assert — проверяем INSERT, Cascade и Auditing

        assertThat(userId).isNotNull();
        assertThat(cardId).isNotNull();

        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        assertThat(card.getCreatedAt()).isNotNull();
        assertThat(card.getUpdatedAt()).isNotNull();

        assertThat(card.getUser()).isSameAs(savedUser);

        // Проверяем обновление updatedAt

        Instant previousUpdatedAt = savedUser.getUpdatedAt();

        Thread.sleep(20);

        savedUser.setName("Petr");
        userRepository.saveAndFlush(savedUser);

        assertThat(savedUser.getUpdatedAt())
                .isAfter(previousUpdatedAt);

        // Очищаем кэш Hibernate, чтобы читать именно из БД

        entityManager.clear();

        User userFromDatabase = userRepository
                .findById(userId)
                .orElseThrow();

        PaymentCard cardFromDatabase = paymentCardRepository
                .findById(cardId)
                .orElseThrow();

        assertThat(userFromDatabase.getName()).isEqualTo("Petr");
        assertThat(cardFromDatabase.getHolder()).isEqualTo("IVAN IVANOV");
        assertThat(cardFromDatabase.getUser().getId()).isEqualTo(userId);

        // Удаляем пользователя

        userRepository.delete(userFromDatabase);
        userRepository.flush();
        entityManager.clear();

        // Карта также должна удалиться благодаря Cascade

        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(paymentCardRepository.findById(cardId)).isEmpty();
    }
}