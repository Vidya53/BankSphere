package com.cts.branchservice.dto.response;

import com.cts.branchservice.enums.BranchStatus;
import com.cts.branchservice.enums.BranchType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BranchResponse {

    private Long branchId;
    private String branchCode;
    private String branchName;
    private BranchType branchType;
    private AddressResponse address;
    private String ifscCode;
    private ContactResponse contact;
    private String branchManagerCode;
    private String branchManagerName;
    private BranchStatus status;
    private Boolean hasAtm;
    private Boolean has24x7Service;
    private Double latitude;
    private Double longitude;
    private String remarks;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressResponse {
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContactResponse {
        private String primaryPhone;
        private String secondaryPhone;
        private String email;
        private String fax;
    }
}
