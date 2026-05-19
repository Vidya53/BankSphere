package com.cts.branchservice.service.impl;

import com.cts.branchservice.dto.request.BranchCreateRequest;
import com.cts.branchservice.dto.request.BranchStatusRequest;
import com.cts.branchservice.dto.request.BranchUpdateRequest;
import com.cts.branchservice.dto.request.OperatingHoursRequest;
import com.cts.branchservice.dto.response.*;
import com.cts.branchservice.entity.Branch;
import com.cts.branchservice.entity.BranchAddress;
import com.cts.branchservice.entity.BranchContact;
import com.cts.branchservice.entity.BranchOperatingHours;
import com.cts.branchservice.entity.Employee;
import com.cts.branchservice.enums.BranchStatus;
import com.cts.branchservice.enums.BranchType;
import com.cts.branchservice.enums.Designation;
import com.cts.branchservice.exception.BranchAlreadyExistsException;
import com.cts.branchservice.exception.BranchNotFoundException;
import com.cts.branchservice.mapper.BranchMapper;
import com.cts.branchservice.repository.BranchOperatingHoursRepository;
import com.cts.branchservice.repository.BranchRepository;
import com.cts.branchservice.repository.EmployeeRepository;
import com.cts.branchservice.service.BranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BranchServiceImpl implements BranchService {

    private static final String BANK_CODE = "BNKS";

    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchOperatingHoursRepository hoursRepository;

    @Override
    public BranchResponse createBranch(BranchCreateRequest request, String createdBy) {
        if (branchRepository.existsByBranchCode(request.getBranchCode())) {
            throw new BranchAlreadyExistsException(
                    "Branch code already exists: " + request.getBranchCode());
        }

        String ifscCode = (request.getIfscCode() != null && !request.getIfscCode().isBlank())
                ? request.getIfscCode().toUpperCase()
                : generateIfsc(request.getBranchCode());

        if (branchRepository.existsByIfscCode(ifscCode)) {
            throw new BranchAlreadyExistsException(
                    "IFSC code is already assigned to another branch: " + ifscCode);
        }

        Branch branch = BranchMapper.toEntity(request, ifscCode, createdBy);
        branch = branchRepository.save(branch);

        if (request.getOperatingHours() != null && !request.getOperatingHours().isEmpty()) {
            persistOperatingHours(branch, request.getOperatingHours());
        }

        log.info("Branch created: code={} ifsc={} by={}", branch.getBranchCode(), ifscCode, createdBy);
        return BranchMapper.toResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchByCode(String branchCode) {
        Branch branch = requireBranch(branchCode);
        BranchResponse response = BranchMapper.toResponse(branch);
        resolveManagerName(branch, response);
        return response;
    }

    @Override
    public BranchResponse updateBranch(String branchCode, BranchUpdateRequest request, String updatedBy) {
        Branch branch = requireBranch(branchCode);

        if (request.getBranchName() != null) branch.setBranchName(request.getBranchName());
        if (request.getBranchType() != null) branch.setBranchType(request.getBranchType());
        if (request.getLatitude() != null) branch.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) branch.setLongitude(request.getLongitude());
        if (request.getHasAtm() != null) branch.setHasAtm(request.getHasAtm());
        if (request.getHas24x7Service() != null) branch.setHas24x7Service(request.getHas24x7Service());
        if (request.getRemarks() != null) branch.setRemarks(request.getRemarks());
        branch.setUpdatedBy(updatedBy);

        if (request.getAddress() != null) {
            BranchCreateRequest.AddressRequest a = request.getAddress();
            BranchAddress addr = branch.getAddress() != null ? branch.getAddress() : new BranchAddress();
            if (a.getAddressLine1() != null) addr.setAddressLine1(a.getAddressLine1());
            if (a.getAddressLine2() != null) addr.setAddressLine2(a.getAddressLine2());
            if (a.getCity() != null) addr.setCity(a.getCity());
            if (a.getState() != null) addr.setState(a.getState());
            if (a.getPostalCode() != null) addr.setPostalCode(a.getPostalCode());
            if (a.getCountry() != null) addr.setCountry(a.getCountry());
            branch.setAddress(addr);
        }

        if (request.getContact() != null) {
            BranchCreateRequest.ContactRequest c = request.getContact();
            BranchContact contact = branch.getContact() != null ? branch.getContact() : new BranchContact();
            if (c.getPrimaryPhone() != null) contact.setPrimaryPhone(c.getPrimaryPhone());
            if (c.getSecondaryPhone() != null) contact.setSecondaryPhone(c.getSecondaryPhone());
            if (c.getEmail() != null) contact.setEmail(c.getEmail());
            if (c.getFax() != null) contact.setFax(c.getFax());
            branch.setContact(contact);
        }

        Branch saved = branchRepository.save(branch);
        log.info("Branch updated: code={} by={}", branchCode, updatedBy);
        return BranchMapper.toResponse(saved);
    }

    @Override
    public void updateBranchStatus(String branchCode, BranchStatusRequest request, String updatedBy) {
        Branch branch = requireBranch(branchCode);
        BranchStatus oldStatus = branch.getStatus();
        branch.setStatus(request.getStatus());
        branch.setUpdatedBy(updatedBy);
        if (request.getReason() != null) {
            branch.setRemarks(request.getReason());
        }
        branchRepository.save(branch);
        log.info("Branch status changed: code={} {}→{} reason='{}' by={}",
                branchCode, oldStatus, request.getStatus(), request.getReason(), updatedBy);
    }

    @Override
    public void deleteBranch(String branchCode, String deletedBy) {
        Branch branch = requireBranch(branchCode);
        branch.setIsDeleted(true);
        branch.setStatus(BranchStatus.INACTIVE);
        branch.setUpdatedBy(deletedBy);
        branchRepository.save(branch);
        log.info("Branch soft-deleted: code={} by={}", branchCode, deletedBy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchResponse> getAllBranches(BranchStatus status, BranchType branchType,
                                               String city, String state, Pageable pageable) {
        return branchRepository.findAllWithFilters(status, branchType, city, state, pageable)
                .map(BranchMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> searchBranches(String query) {
        return BranchMapper.toResponseList(branchRepository.searchBranches(query));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByState(String state) {
        return BranchMapper.toResponseList(
                branchRepository.findAllByAddressStateIgnoreCaseAndIsDeletedFalse(state));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByCity(String city) {
        return BranchMapper.toResponseList(
                branchRepository.findAllByAddressCityIgnoreCaseAndIsDeletedFalse(city));
    }

    @Override
    @Transactional(readOnly = true)
    public BranchSummaryResponse getBranchSummary(String branchCode) {
        Branch branch = requireBranch(branchCode);

        long totalEmployees = employeeRepository.countByBranch_BranchCode(branchCode);
        long activeEmployees = employeeRepository.countByBranch_BranchCodeAndStatus(
                branchCode, com.cts.branchservice.enums.EmployeeStatus.ACTIVE);

        Map<String, Long> byDesignation = buildDesignationCounts(branchCode);

        String managerName = resolveManagerNameString(branch);

        List<OperatingHoursResponse> weeklySchedule = hoursRepository
                .findAllByBranch_BranchCode(branchCode).stream()
                .map(BranchMapper::toHoursResponse)
                .sorted(Comparator.comparing(h -> h.getDayOfWeek().getValue()))
                .toList();

        boolean currentlyOpen = isCurrentlyOpen(branchCode);
        LocalTime[] todayHours = getTodayHours(branchCode);

        return BranchSummaryResponse.builder()
                .branch(BranchMapper.toResponse(branch))
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .employeesByDesignation(byDesignation)
                .branchManagerName(managerName)
                .isCurrentlyOpen(currentlyOpen)
                .todayOpenTime(todayHours[0])
                .todayCloseTime(todayHours[1])
                .weeklySchedule(weeklySchedule)
                .build();
    }

    @Override
    public List<OperatingHoursResponse> setOperatingHours(String branchCode,
                                                          List<OperatingHoursRequest> requests,
                                                          String updatedBy) {
        Branch branch = requireBranch(branchCode);
        persistOperatingHours(branch, requests);
        branch.setUpdatedBy(updatedBy);
        branchRepository.save(branch);
        log.info("Operating hours updated for branch={} by={}", branchCode, updatedBy);
        return hoursRepository.findAllByBranch_BranchCode(branchCode).stream()
                .map(BranchMapper::toHoursResponse)
                .sorted(Comparator.comparing(h -> h.getDayOfWeek().getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperatingHoursResponse> getOperatingHours(String branchCode) {
        requireBranch(branchCode);
        return hoursRepository.findAllByBranch_BranchCode(branchCode).stream()
                .map(BranchMapper::toHoursResponse)
                .sorted(Comparator.comparing(h -> h.getDayOfWeek().getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBranchCurrentlyOpen(String branchCode) {
        requireBranch(branchCode);
        return isCurrentlyOpen(branchCode);
    }

    // ── Internal API ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public boolean isBranchActive(String branchCode) {
        return branchRepository.findByBranchCodeAndIsDeletedFalse(branchCode)
                .map(Branch::isActive)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchValidationResponse getBranchForValidation(String branchCode) {
        return branchRepository.findByBranchCodeAndIsDeletedFalse(branchCode)
                .map(b -> BranchValidationResponse.builder()
                        .branchCode(b.getBranchCode())
                        .branchName(b.getBranchName())
                        .ifscCode(b.getIfscCode())
                        .status(b.getStatus())
                        .isActive(b.isActive())
                        .city(b.getAddress() != null ? b.getAddress().getCity() : null)
                        .state(b.getAddress() != null ? b.getAddress().getState() : null)
                        .country(b.getAddress() != null ? b.getAddress().getCountry() : null)
                        .inactiveReason(b.isActive() ? null :
                                "Branch is " + b.getStatus().name().toLowerCase().replace('_', ' '))
                        .build())
                .orElse(BranchValidationResponse.builder()
                        .branchCode(branchCode)
                        .isActive(false)
                        .inactiveReason("Branch not found")
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public String getIfscCode(String branchCode) {
        return requireBranch(branchCode).getIfscCode();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Branch requireBranch(String branchCode) {
        return branchRepository.findByBranchCodeAndIsDeletedFalse(branchCode)
                .orElseThrow(() -> new BranchNotFoundException(
                        "Branch not found with code: " + branchCode));
    }

    private String generateIfsc(String branchCode) {
        // RBI IFSC format: 4 bank code + 0 + 6 branch identifier = 11 chars
        String normalized = branchCode.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (normalized.length() > 6) normalized = normalized.substring(0, 6);
        while (normalized.length() < 6) normalized += "0";
        return BANK_CODE + "0" + normalized;
    }

    private void persistOperatingHours(Branch branch, List<OperatingHoursRequest> requests) {
        for (OperatingHoursRequest req : requests) {
            BranchOperatingHours existing = hoursRepository
                    .findByBranch_BranchCodeAndDayOfWeek(branch.getBranchCode(), req.getDayOfWeek())
                    .orElse(null);
            hoursRepository.save(BranchMapper.toHoursEntity(existing, req, branch));
        }
    }

    private boolean isCurrentlyOpen(String branchCode) {
        Branch branch = branchRepository.findByBranchCodeAndIsDeletedFalse(branchCode).orElse(null);
        if (branch == null || !branch.isActive()) return false;
        if (Boolean.TRUE.equals(branch.getHas24x7Service())) return true;

        DayOfWeek today = LocalDateTime.now().getDayOfWeek();
        return hoursRepository.findByBranch_BranchCodeAndDayOfWeek(branchCode, today)
                .map(h -> {
                    if (Boolean.TRUE.equals(h.getIsClosed())) return false;
                    LocalTime now = LocalTime.now();
                    return h.getOpenTime() != null && h.getCloseTime() != null
                            && !now.isBefore(h.getOpenTime()) && !now.isAfter(h.getCloseTime());
                })
                .orElse(false);
    }

    private LocalTime[] getTodayHours(String branchCode) {
        DayOfWeek today = LocalDateTime.now().getDayOfWeek();
        return hoursRepository.findByBranch_BranchCodeAndDayOfWeek(branchCode, today)
                .map(h -> new LocalTime[]{h.getOpenTime(), h.getCloseTime()})
                .orElse(new LocalTime[]{null, null});
    }

    private Map<String, Long> buildDesignationCounts(String branchCode) {
        List<Object[]> rows = employeeRepository.countByDesignationForBranch(branchCode);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(((Designation) row[0]).name(), (Long) row[1]);
        }
        return result;
    }

    private void resolveManagerName(Branch branch, BranchResponse response) {
        if (branch.getBranchManagerCode() != null) {
            employeeRepository.findByEmployeeCode(branch.getBranchManagerCode())
                    .ifPresent(mgr -> response.setBranchManagerName(mgr.getFullName()));
        }
    }

    private String resolveManagerNameString(Branch branch) {
        if (branch.getBranchManagerCode() == null) return null;
        return employeeRepository.findByEmployeeCode(branch.getBranchManagerCode())
                .map(Employee::getFullName)
                .orElse(null);
    }
}
