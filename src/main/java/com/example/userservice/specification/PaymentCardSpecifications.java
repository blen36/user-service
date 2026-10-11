package com.example.userservice.specification;

import com.example.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class PaymentCardSpecifications {

    private PaymentCardSpecifications() {
    }

    public static Specification<PaymentCard> userNameContains(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String normalizedName = name
                    .trim()
                    .toLowerCase(Locale.ROOT);

            return criteriaBuilder.like(
                    criteriaBuilder.lower(
                            root.get("user").get("name")
                    ),
                    "%" + normalizedName + "%"
            );
        };
    }

    public static Specification<PaymentCard> userSurnameContains(
            String surname
    ) {
        return (root, query, criteriaBuilder) -> {
            if (surname == null || surname.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String normalizedSurname = surname
                    .trim()
                    .toLowerCase(Locale.ROOT);

            return criteriaBuilder.like(
                    criteriaBuilder.lower(
                            root.get("user").get("surname")
                    ),
                    "%" + normalizedSurname + "%"
            );
        };
    }
}