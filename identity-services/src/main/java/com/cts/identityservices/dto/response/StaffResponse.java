package com.cts.identityservices.dto.response;

import com.cts.identityservices.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class StaffResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String role;
    private String branchCode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public static StaffResponse from(User u) {
        return StaffResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .dateOfBirth(u.getDateOfBirth())
                .role(u.getRole() == null ? null : u.getRole().name())
                .branchCode(u.getBranchCode())
                .status(u.getStatus() == null ? null : u.getStatus().name())
                .createdAt(u.getCreatedAt())
                .lastLogin(u.getLastLogin())
                .build();
    }
}
