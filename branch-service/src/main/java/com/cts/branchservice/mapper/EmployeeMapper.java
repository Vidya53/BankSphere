package com.cts.branchservice.mapper;

import com.cts.branchservice.dto.request.EmployeeRequest;
import com.cts.branchservice.dto.response.EmployeeResponse;
import com.cts.branchservice.entity.Branch;
import com.cts.branchservice.entity.Employee;
import com.cts.branchservice.enums.EmployeeStatus;

import java.util.List;

public final class EmployeeMapper {

    private EmployeeMapper() {}

    public static Employee toEntity(EmployeeRequest req, Branch branch, String employeeCode) {
        return Employee.builder()
                .employeeCode(employeeCode)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .designation(req.getDesignation())
                .department(req.getDepartment())
                .branch(branch)
                .joiningDate(req.getJoiningDate())
                .status(EmployeeStatus.ACTIVE)
                .remarks(req.getRemarks())
                .build();
    }

    public static EmployeeResponse toResponse(Employee employee, String branchManagerCode) {
        boolean isManager = employee.getEmployeeCode() != null
                && employee.getEmployeeCode().equals(branchManagerCode);

        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .designation(employee.getDesignation())
                .department(employee.getDepartment())
                .branchCode(employee.getBranchCode())
                .branchName(employee.getBranch() != null ? employee.getBranch().getBranchName() : null)
                .isBranchManager(isManager)
                .joiningDate(employee.getJoiningDate())
                .status(employee.getStatus())
                .remarks(employee.getRemarks())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    public static List<EmployeeResponse> toResponseList(List<Employee> employees, String branchManagerCode) {
        return employees.stream().map(e -> toResponse(e, branchManagerCode)).toList();
    }
}
