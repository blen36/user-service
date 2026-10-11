package com.example.userservice.controller;

import com.example.userservice.dto.PaymentCardRequestDto;
import com.example.userservice.dto.PaymentCardResponseDto;
import com.example.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    public PaymentCardController(
            PaymentCardService paymentCardService
    ) {
        this.paymentCardService = paymentCardService;
    }

    @PostMapping("/users/{userId}/cards")
    public ResponseEntity<PaymentCardResponseDto> create(
            @PathVariable Long userId,
            @Valid @RequestBody PaymentCardRequestDto request
    ) {
        PaymentCardResponseDto response =
                paymentCardService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<PaymentCardResponseDto> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                paymentCardService.getById(id)
        );
    }

    @GetMapping("/cards")
    public ResponseEntity<Page<PaymentCardResponseDto>> getAll(
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                paymentCardService.getAll(pageable)
        );
    }

    @GetMapping("/users/{userId}/cards")
    public ResponseEntity<Page<PaymentCardResponseDto>> getAllByUserId(
            @PathVariable Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                paymentCardService.getAllByUserId(userId, pageable)
        );
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<PaymentCardResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody PaymentCardRequestDto request
    ) {
        return ResponseEntity.ok(
                paymentCardService.update(id, request)
        );
    }

    @PatchMapping("/cards/{id}/active")
    public ResponseEntity<PaymentCardResponseDto> changeActive(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(
                paymentCardService.changeActive(id, active)
        );
    }

    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        paymentCardService.delete(id);

        return ResponseEntity.noContent().build();
    }
}