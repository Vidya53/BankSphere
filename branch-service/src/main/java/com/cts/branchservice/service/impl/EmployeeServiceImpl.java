package com.cts.branchservice.service.impl;

import com.cts.branchservice.dto.request.EmployeeRequest;
import com.cts.branchservice.dto.request.EmployeeTransferRequest;
import com.cts.branchservice.dto.response.EmployeeResponse;
import com.cts.branchservice.entity.Branch;
import com.cts.branchservice.entity.Employee;
import com.cts.branchservice.enums.BranchStatus;
import com.cts.branchservice.enums.Designation;
import com.cts.branchservice.enums.EmployeeStatus;
import com.cts.branchservice.exception.BranchInactiveException;
import com.cts.branchservice.exception.BranchNotFoundException;
import com.cts.branchservice.exception.EmployeeNotFoundException;
import com.cts.branchservice.mapper.EmployeeMapper;
import com.cts.branchservice.repository.BranchRepository;
import com.cts.branchservice.repository.EmployeeRepository;
import com.cts.branchservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;

    @Override
    public EmployeeResponse addEmployee(String branchCode, EmployeeRequest request) {
        Branch branch = requireActiveBranch(branchCode);

        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An employee with email '" + request.getEmail() + "' already exists");
        }

        String employeeCode = generateEmployeeCode();
        Employee employee = EmployeeMapper.toEntity(request, branch, employeeCode);
        employee = employeeRepository.save(employee);

        log.info("Employee added: code={} branch={}", employeeCode, branchCode);
        return EmployeeMapper.toResponse(employee, branch.getBranchManagerCode());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(String employeeCode) {
        Employee employee = requireEmployee(employeeCode);
        String managerCode = employee.getBranch() != null ? employee.getBranch().getBranchManagerCode() : null;
        return EmployeeMapper.toResponse(employee, managerCode);
    }

    @Override
    public EmployeeResponse updateEmployee(String employeeCode, EmployeeRequest request) {
        Employee employee = requireEmployee(employeeCode);

        if (request.getFirstName() != null) employee.setFirstName(request.getFirstName());
        if (request.getLastName() != null) employee.setLastName(request.getLastName());
        if (request.getPhone() != null) employee.setPhone(request.getPhone());
        if (request.getDesignation() != null) employee.setDesignation(request.getDesignation());
        if (request.getDepartment() != null) employee.setDepartment(request.getDepartment());
        if (request.getJoiningDate() != null) employee.setJoiningDate(request.getJoiningDate());
        if (request.getRemarks() != null) employee.setRemarks(request.getRemarks());

        if (request.getEmail() != null && !request.getEmail().equals(employee.getEmail())) {
            if (employeeRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email '" + request.getEmail() + "' is already in use");
            }
            employee.setEmail(request.getEmail());
        }

        Employee saved = employeeRepository.save(employee);
        log.info("Employee updated: code={}", employeeCode);
        String managerCode = saved.getBranch() != null ? saved.getBranch().getBranchManagerCode() : null;
        return EmployeeMapper.toResponse(saved, managerCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getBranchEmployees(String branchCode, EmployeeStatus status, Pageable pageable) {
        requireBranch(branchCode);
        String managerCode = branchRepository.findByBranchCodeAndIsDeletedFalse(branchCode)
                .map(Branch::getBranchManagerCode).orElse(null);

        Page<Employee> page = (status != null)
                ? employeeRepository.findAllByBranch_BranchCodeAndStatus(branchCode, status, pageable)
                : employeeRepository.findAllByBranch_BranchCode(branchCode, pageable);

        return page.map(e -> EmployeeMapper.toResponse(e, managerCode));
    }

    @Override
    public EmployeeResponse transferEmployee(String employeeCode, EmployeeTransferRequest request) {
        Employee employee = requireEmployee(employeeCode);
        String sourceBranchCode = employee.getBranchCode();
        Branch targetBranch = requireActiveBranch(request.getTargetBranchCode());

        // If this employee is the manager of the source branch, clear the manager reference
        if (sourceBranchCode != null) {
            branchRepository.findByBranchCodeAndIsDeletedFalse(sourceBranchCode).ifPresent(source -> {
                if (employeeCode.equals(source.getBranchManagerCode())) {
                    source.setBranchManagerCode(null);
                    branchRepository.save(source);
                    log.warn("Branch manager {} transferred away from branch {}; manager slot is now vacant",
                            employeeCode, sourceBranchCode);
                }
            });
        }

        employee.setBranch(targetBranch);
        if (request.getTransferReason() != null) employee.setRemarks(request.getTransferReason());
        Employee saved = employeeRepository.save(employee);

        log.info("Employee transferred: code={} from={} to={}", employeeCode, sourceBranchCode, request.getTargetBranchCode());
        return EmployeeMapper.toResponse(saved, targetBranch.getBranchManagerCode());
    }

    @Override
    public void updateEmployeeStatus(String employeeCode, EmployeeStatus status) {
        Employee employee = requireEmployee(employeeCode);
        EmployeeStatus oldStatus = employee.getStatus();
        employee.setStatus(status);
        employeeRepository.save(employee);
        log.info("Employee status updated: code={} {}→{}", employeeCode, oldStatus, status);
    }

    @Override
    public void assignBranchManager(String branchCode, String employeeCode) {
        Branch branch = requireActiveBranch(branchCode);
        Employee employee = requireEmployee(employeeCode);

        if (!branchCode.equals(employee.getBranchCode())) {
            throw new IllegalArgumentException(
                    "Employee " + employeeCode + " does not belong to branch " + branchCode);
        }
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Only ACTIVE employees can be assigned as branch manager");
        }

        branch.setBranchManagerCode(employeeCode);
        // Auto-promote to BRANCH_MANAGER designation if not already a managerial designation
        if (employee.getDesignation() != Designation.BRANCH_MANAGER
                && employee.getDesignation() != Designation.DEPUTY_MANAGER) {
            employee.setDesignation(Designation.BRANCH_MANAGER);
            employeeRepository.save(employee);
        }

        branchRepository.save(branch);
        log.info("Branch manager assigned: branch={} employee={}", branchCode, employeeCode);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getBranchManager(String branchCode) {
        Branch branch = requireBranch(branchCode);
        if (branch.getBranchManagerCode() == null) {
            throw new EmployeeNotFoundException("No branch manager assigned to branch: " + branchCode);
        }
        Employee manager = requireEmployee(branch.getBranchManagerCode());
        return EmployeeMapper.toResponse(manager, branch.getBranchManagerCode());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Employee requireEmployee(String employeeCode) {
        return employeeRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee not found with code: " + employeeCode));
    }

    private Branch requireBranch(String branchCode) {
        return branchRepository.findByBranchCodeAndIsDeletedFalse(branchCode)
                .orElseThrow(() -> new BranchNotFoundException(
                        "Branch not found with code: " + branchCode));
    }

    private Branch requireActiveBranch(String branchCode) {
        Branch branch = requireBranch(branchCode);
        if (branch.getStatus() != BranchStatus.ACTIVE) {
            throw new BranchInactiveException(
                    "Branch " + branchCode + " is not ACTIVE (current status: " + branch.getStatus() + ")");
        }
        return branch;
    }

    private String generateEmployeeCode() {
        Long maxSeq = employeeRepository.findMaxEmployeeSequence();
        long next = (maxSeq == null ? 0L : maxSeq) + 1L;
        return "EMP" + String.format("%06d", next);
    }
}
