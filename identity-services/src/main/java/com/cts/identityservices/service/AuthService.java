package com.cts.identityservices.service;

import com.cts.identityservices.dto.LoginRequest;
import com.cts.identityservices.dto.SignupRequest;


public interface AuthService {

    String signup(SignupRequest request);

    String login(LoginRequest request);
}