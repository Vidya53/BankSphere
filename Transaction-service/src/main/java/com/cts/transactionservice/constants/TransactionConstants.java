package com.cts.transactionservice.constants;

import java.math.BigDecimal;
public final class TransactionConstants {

    private TransactionConstants() {
    }
    public static final String API_BASE_PATH = "/api/v1/transactions";
    public static final String INITIATED_BY_HEADER = "X-Initiated-By";
    public static final String DEFAULT_SYSTEM_USER = "SYSTEM";
    public static final String REFERENCE_PREFIX = "TXN";
    public static final String REVERSAL_IDEMPOTENCY_PREFIX  = "REV-";
    public static final String REFERENCE_DATE_FORMAT = "yyyyMMdd";
    public static final long REFERENCE_RANDOM_MIN = 100_000_000L;
    public static final long REFERENCE_RANDOM_MAX = 999_999_999L;
    public static final BigDecimal DEFAULT_MAX_SINGLE_AMOUNT = new BigDecimal("500000.00");
    public static final BigDecimal DEFAULT_DAILY_TRANSFER_LIMIT = new BigDecimal("1000000.00");
    public static final int DEFAULT_MAX_DAILY_TXN_COUNT = 50;
    public static final int DEFAULT_STALE_PENDING_TIMEOUT_MINS  = 30;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY  = "createdAt";
    public static final String DEFAULT_SORT_DIR = "desc";
    public static final String FAILURE_INSUFFICIENT_FUNDS      = "INSUFFICIENT_FUNDS";
    public static final String FAILURE_ACCOUNT_BLOCKED         = "ACCOUNT_BLOCKED";
    public static final String FAILURE_DAILY_LIMIT_EXCEEDED    = "DAILY_LIMIT_EXCEEDED";
    public static final String FAILURE_INVALID_ACCOUNT         = "INVALID_ACCOUNT";
    public static final String FAILURE_TRANSACTION_TIMED_OUT   = "TRANSACTION_TIMED_OUT";
    public static final String FAILURE_EXTERNAL_GATEWAY_ERROR  = "EXTERNAL_GATEWAY_ERROR";
    public static final String FAILURE_DUPLICATE_TRANSACTION   = "DUPLICATE_TRANSACTION";
    public static final String FAILURE_CURRENCY_MISMATCH       = "CURRENCY_MISMATCH";
    public static final String MSG_TRANSACTION_INITIATED   = "Transaction initiated successfully";
    public static final String MSG_TRANSACTION_FETCHED     = "Transaction fetched successfully";
    public static final String MSG_HISTORY_FETCHED         = "Transaction history fetched successfully";
    public static final String MSG_TRANSACTION_CANCELLED   = "Transaction cancelled successfully";
    public static final String MSG_TRANSACTION_REVERSED    = "Transaction reversed successfully";
    public static final String MSG_TRANSACTION_SUCCESS     = "Transaction marked as successful";
    public static final String MSG_TRANSACTION_FAILED      = "Transaction marked as failed";
    public static final String MSG_COUNT_FETCHED           = "Transaction count fetched successfully";
    public static final String MSG_AMOUNT_FETCHED          = "Total transacted amount fetched successfully";
    public static final String VALID_AMOUNT_REQUIRED       = "Transaction amount is required";
    public static final String VALID_CURRENCY_REQUIRED     = "Currency is required";
    public static final String VALID_IDEMPOTENCY_REQUIRED  = "Idempotency key is required";
    public static final String VALID_TXN_TYPE_REQUIRED     = "Transaction type is required";
    public static final String VALID_CHANNEL_REQUIRED      = "Transaction channel is required";
    public static final String VALID_PAGE_NEGATIVE         = "Page index must not be negative";
    public static final String VALID_PAGE_SIZE_MIN         = "Page size must be at least 1";
    public static final int ACCOUNT_MASK_VISIBLE_CHARS = 4;
    public static final char ACCOUNT_MASK_CHAR = 'X';
}

