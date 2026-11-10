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
import com.innowise.userservice.specification.PaymentCardSpecifications;
import java.time.Instant;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @CacheEvict(cacheNames = "users", key = "#userId")
    @Transactional
    public PaymentCardResponseDto create(
            Long userId,
            PaymentCardRequestDto request
    ) {
        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " was not found"));
        checkActiveCardLimit(userId);

        PaymentCard card = paymentCardMapper.toEntity(request);

        card.setUser(user);

        PaymentCard savedCard =
                paymentCardRepository.save(card);

        return paymentCardMapper.toDto(savedCard);
    }

    public PaymentCardResponseDto getById(Long id) {
        PaymentCard card = findCard(id);

        return paymentCardMapper.toDto(card);
    }

    public Page<PaymentCardResponseDto> getAll(String name, String surname, Pageable pageable) {
        return paymentCardRepository
                .findAll(PaymentCardSpecifications.userNameContains(name)
                        .and(PaymentCardSpecifications.userSurnameContains(surname)), pageable)
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

    @CacheEvict(cacheNames = "users", key = "#result.userId()")
    @Transactional
    public PaymentCardResponseDto update(
            Long id,
            PaymentCardRequestDto request
    ) {
        PaymentCard card = findCard(id);

        paymentCardMapper.updateEntity(request, card);

        return paymentCardMapper.toDto(card);
    }

    @CacheEvict(cacheNames = "users", key = "#result.userId()")
    @Transactional
    public PaymentCardResponseDto changeActive(
            Long id,
            boolean active
    ) {
        User owner = userRepository.lockOwnerOfCard(id)
                .orElseThrow(() -> missingCard(id));
        PaymentCard card = findCard(id);
        if (active && !card.isActive()) {
            checkActiveCardLimit(owner.getId());
        }
        if (paymentCardRepository.updateActive(id, active, Instant.now()) == 0) {
            throw missingCard(id);
        }
        card = findCard(id);

        return paymentCardMapper.toDto(card);
    }

    @CacheEvict(cacheNames = "users", key = "#result")
    @Transactional
    public Long delete(Long id) {
        PaymentCard card = findCard(id);
        paymentCardRepository.delete(card);
        return card.getUser().getId();
    }

    private void checkActiveCardLimit(Long userId) {
        if (paymentCardRepository.countByUser_IdAndActiveTrue(userId) >= MAX_CARDS_PER_USER) {
            throw new CardLimitExceededException("User cannot have more than " + MAX_CARDS_PER_USER + " active cards");
        }
    }

    private ResourceNotFoundException missingCard(Long id) {
        return new ResourceNotFoundException("Payment card with id " + id + " was not found");
    }

    private User findUser(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + id + " was not found"
                ));
    }

    private PaymentCard findCard(Long id) {
        return paymentCardRepository
                .findById(id)
                .orElseThrow(() -> missingCard(id));
    }
}
