package com.cts.branchservice.dto.request;

import com.cts.branchservice.enums.Department;
import com.cts.branchservice.enums.Designation;
import com.cts.branchservice.enums.EmployeeStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]{1,49}$", message = "Letters, spaces, hyphens and apostrophes only")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Za-z][A-Za-z .'-]{1,49}$", message = "Letters, spaces, hyphens and apostrophes only")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10–15 digits")
    private String phone;

    @NotNull(message = "Designation is required")
    private Designation designation;

    @NotNull(message = "Department is required")
    private Department department;

    @NotNull(message = "Joining date is required")
    @PastOrPresent(message = "Joining date must be today or in the past")
    private LocalDate joiningDate;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;
}
