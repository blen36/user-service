package com.innowise.userservice.repository;

import com.innowise.userservice.entity.PaymentCard;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentCardRepository
        extends JpaRepository<PaymentCard, Long>,
        JpaSpecificationExecutor<PaymentCard> {
    boolean existsByIdAndUser_Id(Long id, Long userId);


    Page<PaymentCard> findAllByUser_Id(
            Long userId,
            Pageable pageable
    );

    long countByUser_IdAndActiveTrue(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE payment_cards SET active = :active, updated_at = :now WHERE id = :id", nativeQuery = true)
    int updateActive(@Param("id") Long id, @Param("active") boolean active, @Param("now") Instant now);
}
