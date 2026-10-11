package com.example.userservice.service;

import com.example.userservice.dto.PaymentCardRequestDto;
import com.example.userservice.dto.PaymentCardResponseDto;
import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.mapper.PaymentCardMapper;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class PaymentCardService {

    private static final long MAX_CARDS_PER_USER = 5;

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    public PaymentCardService(
            PaymentCardRepository paymentCardRepository,
            UserRepository userRepository,
            PaymentCardMapper paymentCardMapper
    ) {
        this.paymentCardRepository = paymentCardRepository;
        this.userRepository = userRepository;
        this.paymentCardMapper = paymentCardMapper;
    }

    @Transactional
    public PaymentCardResponseDto create(
            Long userId,
            PaymentCardRequestDto request
    ) {
        User user = findUser(userId);

        long cardCount =
                paymentCardRepository.countByUser_Id(userId);

        if (cardCount >= MAX_CARDS_PER_USER) {
            throw new IllegalStateException(
                    "User cannot have more than 5 cards"
            );
        }

        PaymentCard card = paymentCardMapper.toEntity(request);

        user.addPaymentCard(card);

        PaymentCard savedCard =
                paymentCardRepository.save(card);

        return paymentCardMapper.toDto(savedCard);
    }

    public PaymentCardResponseDto getById(Long id) {
        PaymentCard card = findCard(id);

        return paymentCardMapper.toDto(card);
    }

    public Page<PaymentCardResponseDto> getAll(
            Pageable pageable
    ) {
        return paymentCardRepository
                .findAll(pageable)
                .map(paymentCardMapper::toDto);
    }

    public Page<PaymentCardResponseDto> getAllByUserId(
            Long userId,
            Pageable pageable
    ) {
        findUser(userId);

        return paymentCardRepository
                .findAllByUser_Id(userId, pageable)
                .map(paymentCardMapper::toDto);
    }

    @Transactional
    public PaymentCardResponseDto update(
            Long id,
            PaymentCardRequestDto request
    ) {
        PaymentCard card = findCard(id);

        paymentCardMapper.updateEntity(request, card);

        return paymentCardMapper.toDto(card);
    }

    @Transactional
    public PaymentCardResponseDto changeActive(
            Long id,
            boolean active
    ) {
        PaymentCard card = findCard(id);
        card.setActive(active);

        return paymentCardMapper.toDto(card);
    }

    @Transactional
    public void delete(Long id) {
        PaymentCard card = findCard(id);
        paymentCardRepository.delete(card);
    }

    private User findUser(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "User with id " + id + " was not found"
                ));
    }

    private PaymentCard findCard(Long id) {
        return paymentCardRepository
                .findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Payment card with id " + id + " was not found"
                ));
    }
}