package com.cts.branchservice.service.impl;

import com.cts.branchservice.dto.request.BranchCreateRequest;
import com.cts.branchservice.dto.request.BranchStatusRequest;
import com.cts.branchservice.dto.request.BranchUpdateRequest;
import com.cts.branchservice.dto.response.BranchResponse;
import com.cts.branchservice.dto.response.BranchSummaryResponse;
import com.cts.branchservice.entity.Branch;
import com.cts.branchservice.entity.BranchAddress;
import com.cts.branchservice.entity.BranchContact;
import com.cts.branchservice.entity.BranchOperatingHours;
import com.cts.branchservice.enums.BranchStatus;
import com.cts.branchservice.enums.BranchType;
import com.cts.branchservice.exception.BranchAlreadyExistsException;
import com.cts.branchservice.exception.BranchNotFoundException;
import com.cts.branchservice.repository.BranchOperatingHoursRepository;
import com.cts.branchservice.repository.BranchRepository;
import com.cts.branchservice.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure JUnit 5 + Mockito test for {@link BranchServiceImpl}.
 *
 * No Spring context, no database — every collaborator is mocked.
 * LENIENT strictness avoids UnnecessaryStubbingException on
 * happy-path setups that fail-fast.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BranchServiceImpl — business logic")
class BranchServiceImplTest {

    @Mock private BranchRepository branchRepository;
    @Mock private BranchOperatingHoursRepository hoursRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks private BranchServiceImpl service;

    private BranchCreateRequest validCreate() {
        return BranchCreateRequest.builder()
                .branchCode("BR001")
                .branchName("Main Branch")
                .branchType(BranchType.URBAN)
                .address(BranchCreateRequest.AddressRequest.builder()
                        .addressLine1("123 Main")
                        .city("Hyderabad")
                        .state("TS")
                        .postalCode("500001")
                        .country("India")
                        .build())
                .contact(BranchCreateRequest.ContactRequest.builder()
                        .primaryPhone("9876543210")
                        .email("br1@bank.com")
                        .build())
                .hasAtm(true)
                .has24x7Service(false)
                .build();
    }

    private Branch existingBranch() {
        return Branch.builder()
                .branchId(1L)
                .branchCode("BR001")
                .branchName("Main Branch")
                .branchType(BranchType.URBAN)
                .ifscCode("BNKS0BR0010")
                .status(BranchStatus.ACTIVE)
                .isDeleted(false)
                .has24x7Service(false)
                .hasAtm(true)
                .address(BranchAddress.builder()
                        .addressLine1("123 Main").city("Hyderabad")
                        .state("TS").country("India").postalCode("500001")
                        .build())
                .contact(BranchContact.builder()
                        .primaryPhone("9876543210").email("br1@bank.com")
                        .build())
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  createBranch
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createBranch(...)")
    class CreateBranch {

        @Test
        @DisplayName("happy path — persists branch with auto-generated IFSC")
        void happyPath() {
            BranchCreateRequest req = validCreate();
            when(branchRepository.existsByBranchCode("BR001")).thenReturn(false);
            when(branchRepository.existsByIfscCode(anyString())).thenReturn(false);
            when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> {
                Branch b = inv.getArgument(0);
                b.setBranchId(1L);
                return b;
            });

            BranchResponse resp = service.createBranch(req, "admin");

            assertThat(resp.getBranchCode()).isEqualTo("BR001");
            assertThat(resp.getIfscCode()).startsWith("BNKS0");
            ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
            verify(branchRepository).save(captor.capture());
            assertThat(captor.getValue().getCreatedBy()).isEqualTo("admin");
            assertThat(captor.getValue().getStatus()).isEqualTo(BranchStatus.ACTIVE);
        }

        @Test
        @DisplayName("throws BranchAlreadyExistsException on duplicate branch code")
        void duplicateCode() {
            BranchCreateRequest req = validCreate();
            when(branchRepository.existsByBranchCode("BR001")).thenReturn(true);

            assertThatThrownBy(() -> service.createBranch(req, "admin"))
                    .isInstanceOf(BranchAlreadyExistsException.class)
                    .hasMessageContaining("BR001");

            verify(branchRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BranchAlreadyExistsException on duplicate IFSC code")
        void duplicateIfsc() {
            BranchCreateRequest req = validCreate();
            req.setIfscCode("BNKS0BR0001");
            when(branchRepository.existsByBranchCode("BR001")).thenReturn(false);
            when(branchRepository.existsByIfscCode("BNKS0BR0001")).thenReturn(true);

            assertThatThrownBy(() -> service.createBranch(req, "admin"))
                    .isInstanceOf(BranchAlreadyExistsException.class)
                    .hasMessageContaining("BNKS0BR0001");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getBranchByCode
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getBranchByCode(...)")
    class GetBranchByCode {

        @Test
        @DisplayName("happy path — returns the branch")
        void happyPath() {
            Branch b = existingBranch();
            when(branchRepository.findByBranchCodeAndIsDeletedFalse("BR001"))
                    .thenReturn(Optional.of(b));

            BranchResponse resp = service.getBranchByCode("BR001");

            assertThat(resp.getBranchCode()).isEqualTo("BR001");
            assertThat(resp.getIfscCode()).isEqualTo("BNKS0BR0010");
        }

        @Test
        @DisplayName("throws BranchNotFoundException when missing")
        void notFound() {
            when(branchRepository.findByBranchCodeAndIsDeletedFalse("BR999"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBranchByCode("BR999"))
                    .isInstanceOf(BranchNotFoundException.class)
                    .hasMessageContaining("BR999");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  updateBranch
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateBranch(...)")
    class UpdateBranch {

        @Test
        @DisplayName("happy path — applies updated fields and persists")
        void happyPath() {
            Branch existing = existingBranch();
            BranchUpdateRequest req = BranchUpdateRequest.builder()
                    .branchName("Renamed Branch")
                    .branchType(BranchType.METRO)
                    .hasAtm(false)
                    .has24x7Service(true)
                    .remarks("relocated")
                    .build();
            when(branchRepository.findByBranchCodeAndIsDeletedFalse("BR001"))
                    .thenReturn(Optional.of(existing));
            when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

            BranchResponse resp = service.updateBranch("BR001", req, "csr");

            assertThat(resp.getBranchName()).isEqualTo("Renamed Branch");
            assertThat(resp.getBranchType()).isEqualTo(BranchType.METRO);
            assertThat(existing.getUpdatedBy()).isEqualTo("csr");
        }

        @Test
        @DisplayName("throws BranchNotFoundException when missing")
        void notFound() {
            when(branchRepository.findByBranchCodeAndIsDeletedFalse("BR999"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateBranch("BR999",
                    BranchUpdateRequest.builder().build(), "admin"))
                    .isInstanceOf(BranchNotFoundException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  updateBranchStatus
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateBranchStatus(...)")
    class UpdateBranchStatus {

        @Test
        @DisplayName("happy path — transitions status and persists remarks")
        void happyPath() {
            Branch existing = existingBranch();
            BranchStatusRequest req = BranchStatusRequest.builder()
                    .status(BranchStatus.TEMPORARILY_CLOSED)
                    .reason("flood damage")
                    .build();
            when(branchRepository.findByBranchCodeAndIsDeletedFalse("BR001"))
                    .thenReturn(Optional.of(existing));
            when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

            service.updateBranchStatus("BR001", req, "admin");

            assertThat(existing.getStatus()).isEqualTo(BranchStatus.TEMPORARILY_CLOSED);
            assertThat(existing.getRemarks()).isEqualTo("flood damage");
            assertThat(existing.getUpdatedBy()).isEqualTo("admin");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  deleteBranch
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteBranch(...)")
    class DeleteBranch {

        @Test
        @DisplayName("happy path — soft-deletes the branch")
        void happyPath() {
            Branch existing = existingBranch();
            when(branchRepository.findByBranchCodeAndIsDeletedFalse("BR001"))
                    .thenReturn(Optional.of(existing));
            when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> inv.getArgument(0));

            service.deleteBranch("BR001", "admin");

            assertThat(existing.getIsDeleted()).isTrue();
            assertThat(existing.getStatus()).isEqualTo(BranchStatus.INACTIVE);
            assertThat(existing.getUpdatedBy()).isEqualTo("admin");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  isBranchCurrentlyOpen
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("isBranchCurrentlyOpen(...)")
    class IsBranchCurrentlyOpen {

        @Test
        @DisplayName("returns true when branch is 24x7")
        void open24x7() {
            Branch b = existingBranch();
            b.setHas24x7Service(true);
            when(branchRepository.findByBranchCodeAndIsDeletedFalse("BR001"))
                    .thenReturn(Optional.of(b));

            assertThat(service.isBranchCurrentlyOpen("BR001")).isTrue();
        }

        @Test
        @DisplayName("returns false when today's row is marked isClosed")
        void closedToday() {
            Branch b = existingBranch();
            BranchOperatingHours closedRow = BranchOperatingHours.builder()
                    .branch(b)
                    .dayOfWeek(LocalDateTime.now().getDayOfWeek())
                    .isClosed(true)
                    .build();
            when(branchRepository.findByBranchCodeAndIsDeletedFalse("BR001"))
                    .thenReturn(Optional.of(b));
            when(hoursRepository.findByBranch_BranchCodeAndDayOfWeek("BR001",
                    LocalDateTime.now().getDayOfWeek()))
                    .thenReturn(Optional.of(closedRow));

            assertThat(service.isBranchCurrentlyOpen("BR001")).isFalse();
        }

        @Test
        @DisplayName("throws BranchNotFoundException when branch missing")
        void notFound() {
            when(branchRepository.findByBranchCodeAndIsDeletedFalse("BR999"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.isBranchCurrentlyOpen("BR999"))
                    .isInstanceOf(BranchNotFoundException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  getBranchSummary
    // ────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getBranchSummary(...)")
    class GetBranchSummary {

        @Test
        @DisplayName("happy path — assembles composite summary")
        void happyPath() {
            Branch b = existingBranch();
            when(branchRepository.findByBranchCodeAndIsDeletedFalse("BR001"))
                    .thenReturn(Optional.of(b));
            when(employeeRepository.countByBranch_BranchCode("BR001")).thenReturn(5L);
            when(employeeRepository.countByBranch_BranchCodeAndStatus("BR001",
                    com.cts.branchservice.enums.EmployeeStatus.ACTIVE)).thenReturn(4L);
            when(employeeRepository.countByDesignationForBranch("BR001"))
                    .thenReturn(Collections.emptyList());
            when(hoursRepository.findAllByBranch_BranchCode("BR001"))
                    .thenReturn(List.of(BranchOperatingHours.builder()
                            .dayOfWeek(DayOfWeek.MONDAY)
                            .openTime(LocalTime.of(9, 0))
                            .closeTime(LocalTime.of(17, 0))
                            .isClosed(false)
                            .branch(b)
                            .build()));
            when(hoursRepository.findByBranch_BranchCodeAndDayOfWeek(anyString(), any()))
                    .thenReturn(Optional.empty());

            BranchSummaryResponse resp = service.getBranchSummary("BR001");

            assertThat(resp.getTotalEmployees()).isEqualTo(5);
            assertThat(resp.getActiveEmployees()).isEqualTo(4);
            assertThat(resp.getBranch().getBranchCode()).isEqualTo("BR001");
            assertThat(resp.getWeeklySchedule()).hasSize(1);
        }
    }
}
