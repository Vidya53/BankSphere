package com.cts.identityservices.service.impl;

import com.cts.identityservices.dto.LoginRequest;
import com.cts.identityservices.dto.SignupRequest;
import com.cts.identityservices.entity.Role;
import com.cts.identityservices.entity.Status;
import com.cts.identityservices.entity.User;
import com.cts.identityservices.exception.InvalidCredentialsException;
import com.cts.identityservices.exception.UserNotFoundException;
import com.cts.identityservices.repository.UserRepository;
import com.cts.identityservices.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    @Override
    public String signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new com.identity.exception.UserAlreadyExistsException("Email already registered");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new com.identity.exception.UserAlreadyExistsException("Phone already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(request.getPassword()) // encrypt later
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return "User registered successfully";
    }

    @Override
    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        if (user.getStatus() == Status.BLOCKED) {
            throw new RuntimeException("User is blocked");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return "Login successful";
    }
}