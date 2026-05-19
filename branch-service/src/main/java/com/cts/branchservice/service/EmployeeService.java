package com.cts.branchservice.service;

import com.cts.branchservice.dto.request.EmployeeRequest;
import com.cts.branchservice.dto.request.EmployeeTransferRequest;
import com.cts.branchservice.dto.response.EmployeeResponse;
import com.cts.branchservice.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    EmployeeResponse addEmployee(String branchCode, EmployeeRequest request);

    EmployeeResponse getEmployee(String employeeCode);

    EmployeeResponse updateEmployee(String employeeCode, EmployeeRequest request);

    Page<EmployeeResponse> getBranchEmployees(String branchCode, EmployeeStatus status, Pageable pageable);

    EmployeeResponse transferEmployee(String employeeCode, EmployeeTransferRequest request);

    void updateEmployeeStatus(String employeeCode, EmployeeStatus status);

    void assignBranchManager(String branchCode, String employeeCode);

    EmployeeResponse getBranchManager(String branchCode);
}
