package com.cts.transactionservice.mapper;

import com.cts.transactionservice.dto.request.TransactionRequestDto;
import com.cts.transactionservice.dto.response.TransactionResponseDto;
import com.cts.transactionservice.model.entity.Transaction;
import com.cts.transactionservice.model.enums.TransactionStatus;
import com.cts.transactionservice.util.TransactionUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
@Component
public class TransactionMapper {
    public Transaction toEntity(TransactionRequestDto dto) {
        Objects.requireNonNull(dto, "TransactionRequestDto must not be null");
        return Transaction.builder()
                .senderAccountId(dto.getSenderAccountId())
                .receiverAccountId(dto.getReceiverAccountId())
                .amount(dto.getAmount())
                .currency(TransactionUtils.uppercaseSafe(dto.getCurrency()))
                .fee(TransactionUtils.nullSafeDecimal(dto.getFee()))
                .tax(TransactionUtils.nullSafeDecimal(dto.getTax()))
                .transactionType(dto.getTransactionType())
                .channel(dto.getChannel())
                .idempotencyKey(dto.getIdempotencyKey())
                .parentTransactionId(dto.getParentTransactionId())
                .externalReferenceId(dto.getExternalReferenceId())
                .description(TransactionUtils.trimSafe(dto.getDescription()))
                .remarks(TransactionUtils.trimSafe(dto.getRemarks()))
                .deviceInfo(TransactionUtils.trimSafe(dto.getDeviceInfo()))
                .geolocation(TransactionUtils.trimSafe(dto.getGeolocation()))
                .build();
    }
    public TransactionResponseDto toResponseDto(Transaction entity) {
        return toResponseDto(entity, null);
    }

    /**
     * Viewer-scoped variant: compares the raw (unmasked) sender / receiver
     * account ids against {@code viewerAccountId} BEFORE masking is applied,
     * so we can stamp a definitive CREDIT / DEBIT / SELF direction on the
     * response. The frontend then renders the sign and label directly from
     * this field instead of doing fragile string matches against masked ids.
     *
     * When {@code viewerAccountId} is null/blank, direction stays null —
     * useful for staff-side lookups by transactionId / referenceNumber where
     * there is no single "viewer".
     */
    public TransactionResponseDto toResponseDto(Transaction entity, String viewerAccountId) {
        Objects.requireNonNull(entity, "Transaction entity must not be null");
        String direction = computeDirection(entity, viewerAccountId);
        return TransactionResponseDto.builder()
                .transactionId(entity.getTransactionId())
                .referenceNumber(entity.getReferenceNumber())
                .idempotencyKey(entity.getIdempotencyKey())
                .parentTransactionId(entity.getParentTransactionId())
                .externalReferenceId(entity.getExternalReferenceId())
                .senderAccountId(TransactionUtils.maskAccountId(entity.getSenderAccountId()))
                .receiverAccountId(TransactionUtils.maskAccountId(entity.getReceiverAccountId()))
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .fee(TransactionUtils.nullSafeDecimal(entity.getFee()))
                .tax(TransactionUtils.nullSafeDecimal(entity.getTax()))
                .netAmount(TransactionUtils.computeNetAmount(entity.getAmount(), entity.getFee(), entity.getTax()))
                .senderBalanceAfter(entity.getSenderBalanceAfter())
                .receiverBalanceAfter(entity.getReceiverBalanceAfter())
                .transactionType(entity.getTransactionType())
                .status(entity.getStatus())
                .channel(entity.getChannel())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .initiatedBy(entity.getInitiatedBy())
                .message(resolveStatusMessage(entity))
                .failureReason(resolveFailureReason(entity))
                .direction(direction)
                .build();
    }

    public List<TransactionResponseDto> toResponseDtoList(List<Transaction> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .filter(Objects::nonNull)           // skip any null elements defensively
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    private String computeDirection(Transaction entity, String viewerAccountId) {
        if (viewerAccountId == null || viewerAccountId.isBlank()) {
            return null;
        }
        String viewer = viewerAccountId.trim().toUpperCase();
        String sender   = normaliseId(entity.getSenderAccountId());
        String receiver = normaliseId(entity.getReceiverAccountId());

        boolean isSender   = sender   != null && sender.equals(viewer);
        boolean isReceiver = receiver != null && receiver.equals(viewer);

        if (isSender && isReceiver) return "SELF";

        // Type-driven direction wins when one side is missing (DEPOSIT, WITHDRAWAL,
        // INTEREST, FEE, PAYMENT). Falls back to side membership for TRANSFER /
        // REVERSAL / REFUND which have both legs populated.
        if (entity.getTransactionType() != null) {
            switch (entity.getTransactionType()) {
                case DEPOSIT:
                case INTEREST:
                    return isReceiver ? "CREDIT" : (isSender ? "DEBIT" : "CREDIT");
                case WITHDRAWAL:
                case FEE:
                    return isSender ? "DEBIT" : (isReceiver ? "CREDIT" : "DEBIT");
                case PAYMENT:
                    return isSender ? "DEBIT" : "CREDIT";
                default:
                    // TRANSFER / REVERSAL / REFUND — direction from viewer's seat.
                    break;
            }
        }

        if (isReceiver) return "CREDIT";
        if (isSender)   return "DEBIT";
        return null;
    }

    private String normaliseId(String id) {
        return (id == null || id.isBlank()) ? null : id.trim().toUpperCase();
    }
    public void updateEntityFromDto(TransactionRequestDto dto, Transaction entity) {
        Objects.requireNonNull(dto,    "TransactionRequestDto must not be null");
        Objects.requireNonNull(entity, "Transaction entity must not be null");

        // Only fields that are legitimately editable after creation
        if (dto.getDescription() != null) entity.setDescription(TransactionUtils.trimSafe(dto.getDescription()));
        if (dto.getRemarks()     != null) entity.setRemarks(TransactionUtils.trimSafe(dto.getRemarks()));
        if (dto.getDeviceInfo()  != null) entity.setDeviceInfo(TransactionUtils.trimSafe(dto.getDeviceInfo()));
        if (dto.getGeolocation() != null) entity.setGeolocation(TransactionUtils.trimSafe(dto.getGeolocation()));
        if (dto.getExternalReferenceId() != null) entity.setExternalReferenceId(dto.getExternalReferenceId());
    }
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        int length = accountId.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        String visible = accountId.substring(length - 4);
        String masked  = "X".repeat(length - 4);
        return masked + visible;
    }

    private BigDecimal computeNetAmount(Transaction entity) {
        if (entity.getAmount() == null) {
            return null;
        }
        BigDecimal fee = nullSafeDecimal(entity.getFee());
        BigDecimal tax = nullSafeDecimal(entity.getTax());
        return entity.getAmount().add(fee).add(tax);
    }
    private String resolveStatusMessage(Transaction entity) {
        if (entity.getStatus() == null) return "Status unknown";
        return switch (entity.getStatus()) {
            case PENDING    -> "Transaction received and awaiting processing";
            case PROCESSING -> "Transaction is being processed";
            case SUCCESS    -> "Transaction completed successfully";
            case FAILED     -> "Transaction failed";
            case CANCELLED  -> "Transaction was cancelled";
            case ON_HOLD    -> "Transaction is under review";
            case REVERSED   -> "Transaction has been reversed";
            case TIMED_OUT  -> "Transaction timed out";
        };
    }
    private String resolveFailureReason(Transaction entity) {
        if (entity.getStatus() == TransactionStatus.FAILED) {
            // The remarks field carries the system-set failure reason code
            return entity.getRemarks();
        }
        return null;
    }

    private BigDecimal nullSafeDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String trimSafe(String value) {
        return (value != null) ? value.trim() : null;
    }

    private String uppercaseSafe(String value) {
        return (value != null) ? value.toUpperCase() : null;
    }
}

