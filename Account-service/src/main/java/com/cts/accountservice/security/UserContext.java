package com.cts.accountservice.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserContext {

    private String userId;
    private String username;
    private String role;
    private String branchCode;
    private String customerName;
    private String email;
    private String phone;
}

