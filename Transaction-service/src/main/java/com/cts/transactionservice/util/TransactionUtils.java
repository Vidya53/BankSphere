package com.cts.transactionservice.util;
import com.cts.transactionservice.constants.TransactionConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
public final class TransactionUtils {
    private TransactionUtils() {
        // utility class — no instances
    }
    public static String generateReferenceNumber() {
        String datePart   = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(TransactionConstants.REFERENCE_DATE_FORMAT));
        long   randomPart = ThreadLocalRandom.current()
                .nextLong(TransactionConstants.REFERENCE_RANDOM_MIN,
                          TransactionConstants.REFERENCE_RANDOM_MAX);
        return TransactionConstants.REFERENCE_PREFIX + "-" + datePart + "-" + randomPart;
    }
    public static String buildReversalIdempotencyKey(String originalTransactionId) {
        return TransactionConstants.REVERSAL_IDEMPOTENCY_PREFIX + originalTransactionId;
    }
    public static String maskAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        int length  = accountId.length();
        int visible = TransactionConstants.ACCOUNT_MASK_VISIBLE_CHARS;
        if (length <= visible) {
            return String.valueOf(TransactionConstants.ACCOUNT_MASK_CHAR).repeat(length);
        }
        String visiblePart = accountId.substring(length - visible);
        String maskedPart  = String.valueOf(TransactionConstants.ACCOUNT_MASK_CHAR)
                .repeat(length - visible);
        return maskedPart + visiblePart;
    }
    public static BigDecimal nullSafeDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
    public static BigDecimal computeNetAmount(BigDecimal amount, BigDecimal fee, BigDecimal tax) {
        return nullSafeDecimal(amount)
                .add(nullSafeDecimal(fee))
                .add(nullSafeDecimal(tax))
                .setScale(4, RoundingMode.HALF_UP);
    }
    public static boolean exceedsLimit(BigDecimal amount, BigDecimal limit) {
        if (amount == null || limit == null) return false;
        return amount.compareTo(limit) > 0;
    }
    public static BigDecimal roundMonetary(BigDecimal value) {
        return nullSafeDecimal(value).setScale(4, RoundingMode.HALF_UP);
    }
    public static LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }
    public static LocalDateTime endOfToday() {
        return LocalDate.now().atTime(LocalTime.MAX);
    }
    public static LocalDateTime stalePendingCutoff(int timeoutMinutes) {
        return LocalDateTime.now().minusMinutes(timeoutMinutes);
    }
    public static boolean isValidDateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) return false;
        return !from.isAfter(to);
    }
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    public static String trimSafe(String value) {
        return value != null ? value.trim() : null;
    }
    public static String uppercaseSafe(String value) {
        return value != null ? value.toUpperCase() : null;
    }
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

