package com.cts.customerservices.util;

import com.cts.customerservices.enums.CustomerStatus;

import java.util.Map;
import java.util.Set;

/**
 * Central constants for business rules used across customer-services.
 */
public final class BusinessConstants {

    private BusinessConstants() {}

    // Loan eligibility
    public static final double ANNUAL_INTEREST_RATE = 0.10;
    public static final double MAX_DEBT_INCOME_RATIO = 0.45;
    public static final int MIN_CUSTOMER_AGE = 18;
    public static final int MAX_CUSTOMER_AGE = 100;
    public static final double MIN_INCOME_FOR_LOAN = 10000.0;

    // Document number patterns
    public static final String AADHAR_PATTERN = "^[2-9]{1}[0-9]{11}$";
    public static final String PAN_PATTERN = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$";
    public static final String PASSPORT_PATTERN = "^[A-Z]{1}[0-9]{7}$";
    public static final String VOTER_ID_PATTERN = "^[A-Z]{3}[0-9]{7}$";

    // Valid status transitions
    public static final Map<CustomerStatus, Set<CustomerStatus>> VALID_STATUS_TRANSITIONS = Map.of(
            CustomerStatus.REGISTERED, Set.of(CustomerStatus.ACTIVE, CustomerStatus.CLOSED),
            CustomerStatus.ACTIVE, Set.of(CustomerStatus.INACTIVE, CustomerStatus.BLOCKED, CustomerStatus.CLOSED),
            CustomerStatus.INACTIVE, Set.of(CustomerStatus.ACTIVE, CustomerStatus.CLOSED),
            CustomerStatus.BLOCKED, Set.of(CustomerStatus.ACTIVE, CustomerStatus.CLOSED),
            CustomerStatus.CLOSED, Set.of()
    );
}

