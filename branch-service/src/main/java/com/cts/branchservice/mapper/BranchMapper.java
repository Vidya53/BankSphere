package com.cts.branchservice.mapper;

import com.cts.branchservice.dto.request.BranchCreateRequest;
import com.cts.branchservice.dto.request.OperatingHoursRequest;
import com.cts.branchservice.dto.response.BranchResponse;
import com.cts.branchservice.dto.response.OperatingHoursResponse;
import com.cts.branchservice.entity.Branch;
import com.cts.branchservice.entity.BranchAddress;
import com.cts.branchservice.entity.BranchContact;
import com.cts.branchservice.entity.BranchOperatingHours;
import com.cts.branchservice.enums.BranchStatus;

import java.util.List;

public final class BranchMapper {

    private BranchMapper() {}

    public static Branch toEntity(BranchCreateRequest req, String ifscCode, String createdBy) {
        BranchCreateRequest.AddressRequest a = req.getAddress();
        BranchCreateRequest.ContactRequest c = req.getContact();

        return Branch.builder()
                .branchCode(req.getBranchCode())
                .branchName(req.getBranchName())
                .branchType(req.getBranchType())
                .address(BranchAddress.builder()
                        .addressLine1(a.getAddressLine1())
                        .addressLine2(a.getAddressLine2())
                        .city(a.getCity())
                        .state(a.getState())
                        .postalCode(a.getPostalCode())
                        .country(a.getCountry())
                        .build())
                .ifscCode(ifscCode)
                .contact(BranchContact.builder()
                        .primaryPhone(c.getPrimaryPhone())
                        .secondaryPhone(c.getSecondaryPhone())
                        .email(c.getEmail())
                        .fax(c.getFax())
                        .build())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .hasAtm(req.getHasAtm() != null ? req.getHasAtm() : false)
                .has24x7Service(req.getHas24x7Service() != null ? req.getHas24x7Service() : false)
                .remarks(req.getRemarks())
                .status(BranchStatus.ACTIVE)
                .createdBy(createdBy)
                .build();
    }

    public static BranchResponse toResponse(Branch branch) {
        BranchAddress addr = branch.getAddress();
        BranchContact cont = branch.getContact();

        return BranchResponse.builder()
                .branchId(branch.getBranchId())
                .branchCode(branch.getBranchCode())
                .branchName(branch.getBranchName())
                .branchType(branch.getBranchType())
                .address(addr == null ? null : BranchResponse.AddressResponse.builder()
                        .addressLine1(addr.getAddressLine1())
                        .addressLine2(addr.getAddressLine2())
                        .city(addr.getCity())
                        .state(addr.getState())
                        .postalCode(addr.getPostalCode())
                        .country(addr.getCountry())
                        .build())
                .ifscCode(branch.getIfscCode())
                .contact(cont == null ? null : BranchResponse.ContactResponse.builder()
                        .primaryPhone(cont.getPrimaryPhone())
                        .secondaryPhone(cont.getSecondaryPhone())
                        .email(cont.getEmail())
                        .fax(cont.getFax())
                        .build())
                .branchManagerCode(branch.getBranchManagerCode())
                .status(branch.getStatus())
                .hasAtm(branch.getHasAtm())
                .has24x7Service(branch.getHas24x7Service())
                .latitude(branch.getLatitude())
                .longitude(branch.getLongitude())
                .remarks(branch.getRemarks())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }

    public static List<BranchResponse> toResponseList(List<Branch> branches) {
        return branches.stream().map(BranchMapper::toResponse).toList();
    }

    public static BranchOperatingHours toHoursEntity(BranchOperatingHours existing,
                                                      OperatingHoursRequest req,
                                                      Branch branch) {
        if (existing == null) {
            return BranchOperatingHours.builder()
                    .branch(branch)
                    .dayOfWeek(req.getDayOfWeek())
                    .isClosed(req.getIsClosed())
                    .openTime(Boolean.TRUE.equals(req.getIsClosed()) ? null : req.getOpenTime())
                    .closeTime(Boolean.TRUE.equals(req.getIsClosed()) ? null : req.getCloseTime())
                    .build();
        }
        existing.setIsClosed(req.getIsClosed());
        existing.setOpenTime(Boolean.TRUE.equals(req.getIsClosed()) ? null : req.getOpenTime());
        existing.setCloseTime(Boolean.TRUE.equals(req.getIsClosed()) ? null : req.getCloseTime());
        return existing;
    }

    public static OperatingHoursResponse toHoursResponse(BranchOperatingHours hours) {
        return OperatingHoursResponse.builder()
                .dayOfWeek(hours.getDayOfWeek())
                .isClosed(hours.getIsClosed())
                .openTime(hours.getOpenTime())
                .closeTime(hours.getCloseTime())
                .build();
    }
}
