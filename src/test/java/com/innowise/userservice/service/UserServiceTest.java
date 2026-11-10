package com.innowise.userservice.service;

import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.DuplicateEmailException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUser() {
        UserRequestDto request = request("ivan@example.com");
        User user = user("ivan@example.com");
        UserResponseDto response = response("ivan@example.com", true);

        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(response);

        UserResponseDto result = userService.create(request);

        assertThat(result).isSameAs(response);
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        UserRequestDto request = request("ivan@example.com");
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("User with this email already exists");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldGetUserById() {
        User user = user("ivan@example.com");
        UserResponseDto response = response("ivan@example.com", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(response);

        assertThat(userService.getById(1L)).isSameAs(response);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User with id 999 was not found");
    }

    @Test
    void shouldGetAllUsers() {
        PageRequest pageable = PageRequest.of(0, 10);
        User user = user("ivan@example.com");
        UserResponseDto response = response("ivan@example.com", true);
        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toDto(user)).thenReturn(response);

        Page<UserResponseDto> result = userService.getAll(null, null, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void shouldUpdateUser() {
        UserRequestDto request = request("new@example.com");
        User user = user("old@example.com");
        UserResponseDto response = response("new@example.com", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(userMapper.toDto(user)).thenReturn(response);

        UserResponseDto result = userService.update(1L, request);

        assertThat(result).isSameAs(response);
        verify(userMapper).updateEntity(request, user);
    }

    @Test
    void shouldRejectDuplicateEmailWhenUpdatingUser() {
        UserRequestDto request = request("occupied@example.com");
        User user = user("ivan@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("User with this email already exists");

        verify(userMapper, never()).updateEntity(request, user);
    }

    @Test
    void shouldUpdateUserWhenEmailCaseChanges() {
        UserRequestDto request = request("IVAN@EXAMPLE.COM");
        User user = user("ivan@example.com");
        UserResponseDto response = response("IVAN@EXAMPLE.COM", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(response);

        UserResponseDto result = userService.update(1L, request);

        assertThat(result).isSameAs(response);
        verify(userRepository, never())
                .existsByEmailIgnoreCase(request.email());
        verify(userMapper).updateEntity(request, user);
    }

    @Test
    void shouldChangeUserActiveStatus() {
        User user = user("ivan@example.com");
        UserResponseDto response = response("ivan@example.com", false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(response);

        when(userRepository.updateActive(eq(1L), eq(false), any(Instant.class))).thenReturn(1);
        UserResponseDto result = userService.changeActive(1L, false);

        verify(userRepository).updateActive(eq(1L), eq(false), any(Instant.class));
        assertThat(result).isSameAs(response);
    }

    @Test
    void shouldDeleteUser() {
        User user = user("ivan@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    private UserRequestDto request(String email) {
        return new UserRequestDto(
                "Ivan",
                "Ivanov",
                LocalDate.of(2000, 5, 15),
                email
        );
    }

    private User user(String email) {
        return new User(
                "Ivan",
                "Ivanov",
                LocalDate.of(2000, 5, 15),
                email
        );
    }

    private UserResponseDto response(String email, boolean active) {
        return new UserResponseDto(
                1L,
                "Ivan",
                "Ivanov",
                LocalDate.of(2000, 5, 15),
                email,
                active,
                null,
                null,
                List.of()
        );
    }
}
