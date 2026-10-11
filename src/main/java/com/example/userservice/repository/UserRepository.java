package com.example.userservice.repository;

import com.example.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository
        extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    boolean existsByEmailIgnoreCase(String email);

    //JPQL работает с Java Entity и Java-полями
    @Query("""
            SELECT user
            FROM User user
            WHERE LOWER(user.email) = LOWER(:email)
            """)
    Optional<User> findByEmailUsingJpql(@Param("email") String email);

    //native SQL
    //Этот метод содержит настоящий PostgreSQL SQL
    @Query(
            value = """
                    SELECT *
                    FROM users
                    WHERE active = true
                    ORDER BY id
                    """,
            nativeQuery = true
    )
    List<User> findAllActiveUsingNativeSql();
}