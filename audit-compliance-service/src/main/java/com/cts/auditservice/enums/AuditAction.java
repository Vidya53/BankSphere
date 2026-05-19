package com.cts.auditservice.enums;

/**
 * Canonical list of audit actions across all BankSphere services.
 * Services publish these as String values so they don't need a compile-time
 * dependency on this enum; the audit service accepts any non-blank String.
 */
public enum AuditAction {

    // ── Account ──────────────────────────────────────────────────────────────
    ACCOUNT_APPLICATION_SUBMITTED,
    ACCOUNT_APPLICATION_APPROVED,
    ACCOUNT_APPLICATION_REJECTED,
    ACCOUNT_CREATED,
    ACCOUNT_FROZEN,
    ACCOUNT_UNFROZEN,
    ACCOUNT_CLOSED,
    ACCOUNT_UPDATED,

    // ── Transaction ───────────────────────────────────────────────────────────
    TRANSFER_INITIATED,
    TRANSFER_COMPLETED,
    TRANSFER_FAILED,
    DEPOSIT_MADE,
    WITHDRAWAL_MADE,
    TRANSACTION_REVERSED,

    // ── Loan ──────────────────────────────────────────────────────────────────
    LOAN_APPLIED,
    LOAN_APPROVED,
    LOAN_REJECTED,
    LOAN_DISBURSED,
    LOAN_FORECLOSED,
    EMI_PAID,
    EMI_PAYMENT_FAILED,

    // ── Customer ──────────────────────────────────────────────────────────────
    CUSTOMER_ONBOARDED,
    CUSTOMER_UPDATED,
    KYC_SUBMITTED,
    KYC_APPROVED,
    KYC_REJECTED,

    // ── Identity / Auth ───────────────────────────────────────────────────────
    USER_REGISTERED,
    USER_LOGIN,
    USER_LOGOUT,
    USER_LOGIN_FAILED,
    ROLE_UPDATED,
    PASSWORD_CHANGED,
    PASSWORD_RESET_REQUESTED,
    TOKEN_REVOKED,

    // ── Branch ────────────────────────────────────────────────────────────────
    BRANCH_CREATED,
    BRANCH_UPDATED,
    BRANCH_ACTIVATED,
    BRANCH_DEACTIVATED,
    BRANCH_DELETED,

    // ── Employee ──────────────────────────────────────────────────────────────
    EMPLOYEE_ADDED,
    EMPLOYEE_UPDATED,
    EMPLOYEE_TRANSFERRED,
    EMPLOYEE_STATUS_CHANGED,
    BRANCH_MANAGER_ASSIGNED
}
