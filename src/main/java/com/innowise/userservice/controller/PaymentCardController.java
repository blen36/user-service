package com.innowise.userservice.controller;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.dto.StatusRequestDto;
import com.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    public PaymentCardController(
            PaymentCardService paymentCardService
    ) {
        this.paymentCardService = paymentCardService;
    }

    @PreAuthorize("@resourceAccess.canAccessUser(#p0, authentication)")
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

    @PreAuthorize("@resourceAccess.canAccessCard(#p0, authentication)")
    @GetMapping("/cards/{id}")
    public ResponseEntity<PaymentCardResponseDto> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                paymentCardService.getById(id)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/cards")
    public ResponseEntity<Page<PaymentCardResponseDto>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                paymentCardService.getAll(name, surname, pageable)
        );
    }

    @PreAuthorize("@resourceAccess.canAccessUser(#p0, authentication)")
    @GetMapping("/users/{userId}/cards")
    public ResponseEntity<Page<PaymentCardResponseDto>> getAllByUserId(
            @PathVariable Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                paymentCardService.getAllByUserId(userId, pageable)
        );
    }

    @PreAuthorize("@resourceAccess.canAccessCard(#p0, authentication)")
    @PutMapping("/cards/{id}")
    public ResponseEntity<PaymentCardResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody PaymentCardRequestDto request
    ) {
        return ResponseEntity.ok(
                paymentCardService.update(id, request)
        );
    }

    @PreAuthorize("@resourceAccess.canAccessCard(#p0, authentication)")
    @PatchMapping("/cards/{id}/status")
    public ResponseEntity<PaymentCardResponseDto> changeActive(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequestDto status
    ) {
        return ResponseEntity.ok(
                paymentCardService.changeActive(id, status.active())
        );
    }

    @PreAuthorize("@resourceAccess.canAccessCard(#p0, authentication)")
    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        paymentCardService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
