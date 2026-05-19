package com.cts.branchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "branch_operating_hours",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_branch_day",
                columnNames = {"branch_id", "day_of_week"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchOperatingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    // null openTime / closeTime means the branch is closed on this day
    private LocalTime openTime;
    private LocalTime closeTime;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isClosed = false;
}
