package com.innowise.userservice.repository;

import com.innowise.userservice.entity.User;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository
        extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {
    boolean existsByIdAndActiveTrue(Long id);


    boolean existsByEmailIgnoreCase(String email);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = (select c.user.id from PaymentCard c where c.id = :id)")
    Optional<User> lockOwnerOfCard(@Param("id") Long cardId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.active = :active, u.updatedAt = :now where u.id = :id")
    int updateActive(@Param("id") Long id, @Param("active") boolean active, @Param("now") Instant now);
}
