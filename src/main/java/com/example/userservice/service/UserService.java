package com.example.userservice.service;

import com.example.userservice.dto.UserRequestDto;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.entity.User;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.ResourceNotFoundException;

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

    public UserResponseDto getById(Long id) {
        User user = findUser(id);

        return userMapper.toDto(user);
    }

    public Page<UserResponseDto> getAll(Pageable pageable) {
        return userRepository
                .findAll(pageable)
                .map(userMapper::toDto);
    }

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

    @Transactional
    public UserResponseDto changeActive(
            Long id,
            boolean active
    ) {
        User user = findUser(id);
        user.setActive(active);

        return userMapper.toDto(user);
    }

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