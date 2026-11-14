package com.innowise.userservice.repository;

import com.innowise.userservice.dto.OrderUserProfile;
import com.innowise.userservice.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface OrderUserLookup extends Repository<User, Long> {
    @Query("""
            select new com.innowise.userservice.dto.OrderUserProfile(u.id, u.name, u.surname, u.email)
            from User u where lower(u.email) = lower(:email)
            and (:admin = true or (u.id = :caller and u.active = true))
            """)
    Optional<OrderUserProfile> byEmail(@Param("email") String email, @Param("caller") long caller,
                                       @Param("admin") boolean admin);

    @Query("""
            select new com.innowise.userservice.dto.OrderUserProfile(u.id, u.name, u.surname, u.email)
            from User u where u.id = :id
            and (:admin = true or (u.id = :caller and u.active = true))
            """)
    Optional<OrderUserProfile> byId(@Param("id") long id, @Param("caller") long caller,
                                    @Param("admin") boolean admin);
}
