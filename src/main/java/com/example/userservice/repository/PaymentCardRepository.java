package com.example.userservice.repository;

import com.example.userservice.entity.PaymentCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCardRepository
        extends JpaRepository<PaymentCard, Long>,
        JpaSpecificationExecutor<PaymentCard> {

    Page<PaymentCard> findAllByUser_Id(
            Long userId,
            Pageable pageable
    );

    long countByUser_Id(Long userId);

    @Query("""
            SELECT card
            FROM PaymentCard card
            WHERE card.user.id = :userId
              AND card.active = true
            ORDER BY card.id
            """)
    List<PaymentCard> findActiveCardsByUserIdUsingJpql(
            @Param("userId") Long userId
    );
}