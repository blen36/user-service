package com.innowise.userservice.service;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.entity.PaymentCard;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.CardLimitExceededException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @InjectMocks
    private PaymentCardService paymentCardService;

    @Test
    void shouldCreatePaymentCard() {
        User user = user();
        PaymentCardRequestDto request = request("IVAN IVANOV");
        PaymentCard card = card("IVAN IVANOV");
        PaymentCardResponseDto response = response("IVAN IVANOV", true);

        when(userRepository.lockById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUser_IdAndActiveTrue(1L)).thenReturn(0L);
        when(paymentCardMapper.toEntity(request)).thenReturn(card);
        when(paymentCardRepository.save(card)).thenReturn(card);
        when(paymentCardMapper.toDto(card)).thenReturn(response);

        PaymentCardResponseDto result = paymentCardService.create(1L, request);

        assertThat(result).isSameAs(response);
        assertThat(card.getUser()).isSameAs(user);
        verify(paymentCardRepository).save(card);
    }

    @Test
    void shouldRejectSixthPaymentCard() {
        PaymentCardRequestDto request = request("IVAN IVANOV");
        when(userRepository.lockById(1L)).thenReturn(Optional.of(user()));
        when(paymentCardRepository.countByUser_IdAndActiveTrue(1L)).thenReturn(5L);

        assertThatThrownBy(() -> paymentCardService.create(1L, request))
                .isInstanceOf(CardLimitExceededException.class)
                .hasMessage("User cannot have more than 5 active cards");

        verify(paymentCardRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectPaymentCardWhenUserNotFound() {
        PaymentCardRequestDto request = request("IVAN IVANOV");
        when(userRepository.lockById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentCardService.create(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id 999 was not found");

        verify(paymentCardRepository, never()).countByUser_IdAndActiveTrue(999L);
    }

    @Test
    void shouldGetPaymentCardById() {
        PaymentCard card = card("IVAN IVANOV");
        PaymentCardResponseDto response = response("IVAN IVANOV", true);
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toDto(card)).thenReturn(response);

        assertThat(paymentCardService.getById(10L)).isSameAs(response);
    }

    @Test
    void shouldThrowExceptionWhenPaymentCardNotFound() {
        when(paymentCardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentCardService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment card with id 999 was not found");
    }

    @Test
    void shouldGetAllPaymentCards() {
        PageRequest pageable = PageRequest.of(0, 10);
        PaymentCard card = card("IVAN IVANOV");
        PaymentCardResponseDto response = response("IVAN IVANOV", true);
        when(paymentCardRepository.findAll(ArgumentMatchers.<Specification<PaymentCard>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(card)));
        when(paymentCardMapper.toDto(card)).thenReturn(response);

        Page<PaymentCardResponseDto> result = paymentCardService.getAll(null, null, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void shouldGetAllPaymentCardsByUserId() {
        PageRequest pageable = PageRequest.of(0, 10);
        PaymentCard card = card("IVAN IVANOV");
        PaymentCardResponseDto response = response("IVAN IVANOV", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(paymentCardRepository.findAllByUser_Id(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(card)));
        when(paymentCardMapper.toDto(card)).thenReturn(response);

        Page<PaymentCardResponseDto> result =
                paymentCardService.getAllByUserId(1L, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void shouldUpdatePaymentCard() {
        PaymentCardRequestDto request = request("PETR PETROV");
        PaymentCard card = card("IVAN IVANOV");
        PaymentCardResponseDto response = response("PETR PETROV", true);
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toDto(card)).thenReturn(response);

        PaymentCardResponseDto result = paymentCardService.update(10L, request);

        assertThat(result).isSameAs(response);
        verify(paymentCardMapper).updateEntity(request, card);
    }

    @Test
    void shouldChangePaymentCardActiveStatus() {
        PaymentCard card = card("IVAN IVANOV");
        PaymentCardResponseDto response = response("IVAN IVANOV", false);
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(paymentCardMapper.toDto(card)).thenReturn(response);

        when(userRepository.lockOwnerOfCard(10L)).thenReturn(Optional.of(user()));
        when(paymentCardRepository.updateActive(eq(10L), eq(false), any(Instant.class))).thenReturn(1);
        PaymentCardResponseDto result =
                paymentCardService.changeActive(10L, false);

        verify(paymentCardRepository).updateActive(eq(10L), eq(false), any(Instant.class));
        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldDeletePaymentCard() {
        PaymentCard card = card("IVAN IVANOV");
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));

        card.setUser(user());
        paymentCardService.delete(10L);

        verify(paymentCardRepository).delete(card);
    }

    private User user() {
        return new User(
                "Ivan",
                "Ivanov",
                LocalDate.of(2000, 5, 15),
                "ivan@example.com"
        );
    }

    private PaymentCardRequestDto request(String holder) {
        return new PaymentCardRequestDto(
                "4111111111111111",
                holder,
                LocalDate.of(2099, 12, 31)
        );
    }

    private PaymentCard card(String holder) {
        return new PaymentCard(
                "4111111111111111",
                holder,
                LocalDate.of(2099, 12, 31)
        );
    }

    private PaymentCardResponseDto response(String holder, boolean active) {
        return new PaymentCardResponseDto(
                10L,
                1L,
                "4111111111111111",
                holder,
                LocalDate.of(2099, 12, 31),
                active,
                null,
                null
        );
    }
}
