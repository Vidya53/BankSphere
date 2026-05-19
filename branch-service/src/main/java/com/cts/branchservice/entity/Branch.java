package com.cts.branchservice.entity;

import com.cts.branchservice.enums.BranchStatus;
import com.cts.branchservice.enums.BranchType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "branches", indexes = {
        @Index(name = "idx_branch_code", columnList = "branchCode"),
        @Index(name = "idx_branch_status", columnList = "status"),
        @Index(name = "idx_branch_state", columnList = "state")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long branchId;

    @Column(unique = true, nullable = false, length = 10)
    private String branchCode;

    @Column(nullable = false, length = 100)
    private String branchName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BranchType branchType;

    @Embedded
    private BranchAddress address;

    // RBI-compliant IFSC code: 4-char bank code + 0 + 6-char branch code = 11 chars
    @Column(unique = true, nullable = false, length = 11)
    private String ifscCode;

    @Embedded
    private BranchContact contact;

    // References Employee.employeeCode — maintained as a code to avoid circular dependency
    @Column(length = 20)
    private String branchManagerCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BranchStatus status = BranchStatus.ACTIVE;

    // Geolocation for branch locator feature
    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasAtm = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean has24x7Service = false;

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    private String createdBy;
    private String updatedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<BranchOperatingHours> operatingHours = new ArrayList<>();

    public boolean isActive() {
        return BranchStatus.ACTIVE == this.status && Boolean.FALSE.equals(this.isDeleted);
    }
}
