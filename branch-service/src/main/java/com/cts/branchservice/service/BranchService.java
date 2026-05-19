package com.cts.branchservice.service;

import com.cts.branchservice.dto.request.BranchCreateRequest;
import com.cts.branchservice.dto.request.BranchStatusRequest;
import com.cts.branchservice.dto.request.BranchUpdateRequest;
import com.cts.branchservice.dto.request.OperatingHoursRequest;
import com.cts.branchservice.dto.response.BranchResponse;
import com.cts.branchservice.dto.response.BranchSummaryResponse;
import com.cts.branchservice.dto.response.BranchValidationResponse;
import com.cts.branchservice.dto.response.OperatingHoursResponse;
import com.cts.branchservice.enums.BranchStatus;
import com.cts.branchservice.enums.BranchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BranchService {

    BranchResponse createBranch(BranchCreateRequest request, String createdBy);

    BranchResponse getBranchByCode(String branchCode);

    BranchResponse updateBranch(String branchCode, BranchUpdateRequest request, String updatedBy);

    void updateBranchStatus(String branchCode, BranchStatusRequest request, String updatedBy);

    void deleteBranch(String branchCode, String deletedBy);

    Page<BranchResponse> getAllBranches(BranchStatus status, BranchType branchType,
                                        String city, String state, Pageable pageable);

    List<BranchResponse> searchBranches(String query);

    List<BranchResponse> getBranchesByState(String state);

    List<BranchResponse> getBranchesByCity(String city);

    BranchSummaryResponse getBranchSummary(String branchCode);

    List<OperatingHoursResponse> setOperatingHours(String branchCode, List<OperatingHoursRequest> requests, String updatedBy);

    List<OperatingHoursResponse> getOperatingHours(String branchCode);

    boolean isBranchCurrentlyOpen(String branchCode);

    // ── Internal API (called by other microservices) ─────────────────────────

    boolean isBranchActive(String branchCode);

    BranchValidationResponse getBranchForValidation(String branchCode);

    String getIfscCode(String branchCode);
}
