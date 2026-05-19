package com.cts.branchservice.dto.response;

import com.cts.branchservice.enums.Department;
import com.cts.branchservice.enums.Designation;
import com.cts.branchservice.enums.EmployeeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeResponse {

    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private Designation designation;
    private Department department;
    private String branchCode;
    private String branchName;
    private Boolean isBranchManager;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joiningDate;

    private EmployeeStatus status;
    private String remarks;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
