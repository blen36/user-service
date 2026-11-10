package com.innowise.userservice.service;

import com.innowise.userservice.dto.UserRequestDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.DuplicateEmailException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.specification.UserSpecifications;
import java.time.Instant;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponseDto create(UserRequestDto request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateEmailException(
                    "User with this email already exists"
            );
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        return userMapper.toDto(savedUser);
    }

    @Cacheable(cacheNames = "users", key = "#id")
    public UserResponseDto getById(Long id) {
        User user = findUser(id);

        return userMapper.toDto(user);
    }

    public Page<UserResponseDto> getAll(String name, String surname, Pageable pageable) {
        return userRepository
                .findAll(UserSpecifications.nameContains(name)
                        .and(UserSpecifications.surnameContains(surname)), pageable)
                .map(userMapper::toDto);
    }

    @CacheEvict(cacheNames = "users", key = "#id")
    @Transactional
    public UserResponseDto update(
            Long id,
            UserRequestDto request
    ) {
        User user = findUser(id);

        boolean emailChanged =
                !user.getEmail().equalsIgnoreCase(request.email());

        if (emailChanged
                && userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateEmailException(
                    "User with this email already exists"
            );
        }

        userMapper.updateEntity(request, user);

        return userMapper.toDto(user);
    }

    @CacheEvict(cacheNames = "users", key = "#id")
    @Transactional
    public UserResponseDto changeActive(
            Long id,
            boolean active
    ) {
        if (userRepository.updateActive(id, active, Instant.now()) == 0) {
            throw new ResourceNotFoundException("User with id " + id + " was not found");
        }
        User user = findUser(id);

        return userMapper.toDto(user);
    }

    @CacheEvict(cacheNames = "users", key = "#id")
    @Transactional
    public void delete(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
    }

    private User findUser(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + id + " was not found"
                ));
    }
}
