package com.example.userservice.service;

import com.example.userservice.dto.PaymentCardRequestDto;
import com.example.userservice.dto.PaymentCardResponseDto;
import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.exception.CardLimitExceededException;
import com.example.userservice.mapper.PaymentCardMapper;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.countByUser_Id(1L)).thenReturn(0L);
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(paymentCardRepository.countByUser_Id(1L)).thenReturn(5L);

        assertThatThrownBy(() -> paymentCardService.create(1L, request))
                .isInstanceOf(CardLimitExceededException.class)
                .hasMessage("User cannot have more than 5 cards");

        verify(paymentCardRepository, never()).save(org.mockito.ArgumentMatchers.any());
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
    void shouldGetAllPaymentCards() {
        PageRequest pageable = PageRequest.of(0, 10);
        PaymentCard card = card("IVAN IVANOV");
        PaymentCardResponseDto response = response("IVAN IVANOV", true);
        when(paymentCardRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(card)));
        when(paymentCardMapper.toDto(card)).thenReturn(response);

        Page<PaymentCardResponseDto> result = paymentCardService.getAll(pageable);

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

        PaymentCardResponseDto result =
                paymentCardService.changeActive(10L, false);

        assertThat(card.isActive()).isFalse();
        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldDeletePaymentCard() {
        PaymentCard card = card("IVAN IVANOV");
        when(paymentCardRepository.findById(10L)).thenReturn(Optional.of(card));

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
