package com.example.userservice.specification;

import com.example.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> nameContains(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String normalizedName = name
                    .trim()
                    .toLowerCase(Locale.ROOT);

            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + normalizedName + "%"
            );
        };
    }

    public static Specification<User> surnameContains(String surname) {
        return (root, query, criteriaBuilder) -> {
            if (surname == null || surname.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            String normalizedSurname = surname
                    .trim()
                    .toLowerCase(Locale.ROOT);

            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("surname")),
                    "%" + normalizedSurname + "%"
            );
        };
    }
}