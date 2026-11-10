package com.innowise.userservice.specification;

import com.innowise.userservice.entity.PaymentCard;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

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
