package com.cts.transactionservice.controller;

import com.cts.transactionservice.dto.request.CancelTransactionRequestDto;
import com.cts.transactionservice.dto.request.ReverseTransactionRequestDto;
import com.cts.transactionservice.dto.request.TransactionRequestDto;
import com.cts.transactionservice.dto.response.ApiResponse;
import com.cts.transactionservice.dto.response.TransactionResponseDto;
import com.cts.transactionservice.exception.BadRequestException;
import com.cts.transactionservice.model.enums.TransactionStatus;
import com.cts.transactionservice.model.enums.TransactionType;
import com.cts.transactionservice.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('CUSTOMER','CSR','BRANCH_MANAGER','LOAN_OFFICER','ADMIN')")
@Tag(
    name = "Transactions",
    description = "Endpoints for initiating, querying, and managing financial transactions in BankSphere"
)
public class TransactionController {
    private final TransactionService transactionService;
    private static final String INITIATED_BY_HEADER = "X-Initiated-By";
    @Operation(
        summary = "Initiate a new transaction",
        description = """
                Creates a new financial transaction (DEPOSIT, WITHDRAWAL, TRANSFER, etc.). A client-supplied idempotency key is mandatory — duplicate keys return 409. Newly created transactions start in PENDING state and are subject to per-account daily caps of ₹10,00,000 / 50 txns and a per-transaction ceiling of ₹5,00,000.

                **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN
                **Side effects:** Persists a PENDING transaction; downstream payment engine drives it to SUCCESS/FAILED."""
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Transaction initiated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate idempotency key", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Business rule violation", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponseDto>> initiateTransaction(
            @Valid @RequestBody TransactionRequestDto requestDto,
            @RequestHeader(value = INITIATED_BY_HEADER, defaultValue = "SYSTEM") String initiatedBy) {
        log.info("POST /api/v1/transactions | type={} | initiatedBy={}",
                requestDto.getTransactionType(), initiatedBy);
        TransactionResponseDto response = transactionService.initiateTransaction(requestDto, initiatedBy);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Transaction initiated successfully"));
    }
    @Operation(
            summary = "Get transaction by ID",
            description = """
                    Fetches a single transaction by its internal UUID. Returns full transaction details including status, amounts, and timestamps.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransactionById(
            @Parameter(description = "Internal UUID of the transaction") @PathVariable @NotBlank(message = "Transaction ID must not be blank") String transactionId) {
        log.debug("GET /api/v1/transactions/{}", transactionId);
        TransactionResponseDto response = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction fetched successfully"));
    }
    @Operation(
            summary = "Get transaction by reference number",
            description = """
                    Fetches a transaction by its human-readable reference number (e.g., `TXN-20260429-123456789`). Useful for customer-facing lookups when the internal UUID is unknown.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/reference/{referenceNumber}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransactionByReferenceNumber(
            @Parameter(description = "Human-readable reference number") @PathVariable @NotBlank(message = "Reference number must not be blank") String referenceNumber) {
        log.debug("GET /api/v1/transactions/reference/{}", referenceNumber);
        TransactionResponseDto response = transactionService.getTransactionByReferenceNumber(referenceNumber);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction fetched successfully"));
    }
    @Operation(
            summary = "Get transaction by idempotency key",
            description = """
                    Allows clients to safely retry by checking the outcome of a previously submitted request using its idempotency key. Returns the existing transaction instead of creating a new one.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/idempotency/{idempotencyKey}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransactionByIdempotencyKey(
            @Parameter(description = "Client-supplied idempotency key") @PathVariable @NotBlank(message = "Idempotency key must not be blank") String idempotencyKey) {
        log.debug("GET /api/v1/transactions/idempotency/{}", idempotencyKey);
        TransactionResponseDto response = transactionService.getTransactionByIdempotencyKey(idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction fetched successfully"));
    }
    @Operation(
            summary = "Get account transaction history",
            description = """
                    Returns paginated transaction history for an account, covering activity both as sender and receiver. Results are sorted newest-first by default and capped at 100 rows per page.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getTransactionsByAccount(
            @Parameter(description = "Account ID") @PathVariable @NotBlank(message = "Account ID must not be blank") String accountId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        log.debug("GET /api/v1/transactions/account/{} | page={} size={}", accountId, page, size);
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<TransactionResponseDto> response =
                transactionService.getTransactionsByAccountId(accountId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Transaction history fetched successfully"));
    }
    @Operation(
            summary = "Get account history by date range",
            description = """
                    Returns paginated transaction history for an account within the specified ISO-8601 datetime range. Primary use case is bank-statement generation and dispute investigation.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    @GetMapping("/account/{accountId}/range")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getTransactionsByAccountAndDateRange(
            @Parameter(description = "Account ID") @PathVariable @NotBlank(message = "Account ID must not be blank") String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        log.debug("GET /api/v1/transactions/account/{}/range | from={} to={}", accountId, from, to);
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<TransactionResponseDto> response =
                transactionService.getTransactionsByAccountIdAndDateRange(accountId, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Transaction history fetched successfully"));
    }
    @Operation(
            summary = "Filter transactions by status",
            description = """
                    Returns paginated transactions filtered by lifecycle status (PENDING, SUCCESS, FAILED, CANCELLED, REVERSED). Used by ops dashboards and reconciliation jobs.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getTransactionsByStatus(
            @Parameter(description = "Transaction lifecycle status") @PathVariable TransactionStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        log.debug("GET /api/v1/transactions/status/{}", status);
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<TransactionResponseDto> response =
                transactionService.getTransactionsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Transactions fetched by status successfully"));
    }
    @Operation(
            summary = "Filter transactions by account and type",
            description = """
                    Returns paginated transactions for a sender account filtered by transaction type (e.g., all WITHDRAWALs or all TRANSFERs). Useful for type-specific statements and analytics drill-downs.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    @GetMapping("/account/{accountId}/type")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getTransactionsByAccountAndType(
            @Parameter(description = "Sender account ID") @PathVariable @NotBlank(message = "Account ID must not be blank") String accountId,
            @RequestParam TransactionType transactionType,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {
        log.debug("GET /api/v1/transactions/account/{}/type | type={}", accountId, transactionType);
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<TransactionResponseDto> response =
                transactionService.getTransactionsByAccountAndType(accountId, transactionType, pageable);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Transactions fetched by type successfully"));
    }
    @Operation(
            summary = "Cancel a transaction",
            description = """
                    Cancels a PENDING transaction. Only transactions still in PENDING state are cancellable — SUCCESS / FAILED / REVERSED transactions cannot be cancelled and return 409.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN
                    **Side effects:** Transitions PENDING → CANCELLED with the supplied remarks."""
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction cancelled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Transaction not in cancellable state", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PatchMapping("/{transactionId}/cancel")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> cancelTransaction(
            @Parameter(description = "UUID of the transaction to cancel") @PathVariable @NotBlank(message = "Transaction ID must not be blank") String transactionId,
            @Valid @RequestBody CancelTransactionRequestDto requestDto) {

        log.info("PATCH /api/v1/transactions/{}/cancel", transactionId);

        TransactionResponseDto response =
                transactionService.cancelTransaction(transactionId, requestDto.getRemarks());

        return ResponseEntity.ok(ApiResponse.success(response, "Transaction cancelled successfully"));
    }
    @Operation(
            summary = "Reverse a transaction",
            description = """
                    Reverses a transaction that has already completed. Only transactions in SUCCESS state are reversible — a linked REVERSAL transaction is created and the original is moved to REVERSED.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN
                    **Side effects:** Creates a new REVERSAL transaction; transitions original SUCCESS → REVERSED."""
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reversal transaction created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Transaction not reversible or already reversed", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PatchMapping("/{transactionId}/reverse")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> reverseTransaction(
            @Parameter(description = "UUID of the original transaction to reverse") @PathVariable @NotBlank(message = "Transaction ID must not be blank") String transactionId,
            @Valid @RequestBody ReverseTransactionRequestDto requestDto,
            @RequestHeader(value = INITIATED_BY_HEADER, defaultValue = "SYSTEM") String initiatedBy) {
        log.info("PATCH /api/v1/transactions/{}/reverse | initiatedBy={}", transactionId, initiatedBy);
        TransactionResponseDto response =
                transactionService.reverseTransaction(transactionId, requestDto.getRemarks(), initiatedBy);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction reversed successfully"));
    }
    @Operation(
            summary = "Mark transaction as SUCCESS (internal)",
            description = """
                    Called by the payment-processing engine after network confirmation to transition a PENDING transaction to SUCCESS, recording post-settlement sender and receiver balances.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN
                    **Side effects:** Transitions PENDING → SUCCESS and stores running balances."""
    )
    @PatchMapping("/{transactionId}/success")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> markAsSuccess(
            @Parameter(description = "UUID of the transaction") @PathVariable @NotBlank(message = "Transaction ID must not be blank") String transactionId,
            @RequestParam BigDecimal senderBalance,
            @RequestParam BigDecimal receiverBalance) {
        log.info("PATCH /api/v1/transactions/{}/success", transactionId);
        if (senderBalance != null && senderBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("senderBalance cannot be negative.");
        }
        if (receiverBalance != null && receiverBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("receiverBalance cannot be negative.");
        }
        TransactionResponseDto response =
                transactionService.markAsSuccess(transactionId, senderBalance, receiverBalance);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction marked as successful"));
    }
    @Operation(
            summary = "Mark transaction as FAILED (internal)",
            description = """
                    Called by the payment-processing engine on failure to transition a PENDING transaction to FAILED with a descriptive failure reason.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN
                    **Side effects:** Transitions PENDING → FAILED and persists the failure reason."""
    )
    @PatchMapping("/{transactionId}/fail")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> markAsFailed(
            @Parameter(description = "UUID of the transaction") @PathVariable @NotBlank(message = "Transaction ID must not be blank") String transactionId,
            @RequestParam @NotBlank(message = "Failure reason is required") String failureReason) {
        log.info("PATCH /api/v1/transactions/{}/fail | reason={}", transactionId, failureReason);
        TransactionResponseDto response =
                transactionService.markAsFailed(transactionId, failureReason);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction marked as failed"));
    }
    @Operation(
            summary = "Get total transacted amount",
            description = """
                    Returns the cumulative SUCCESS transaction amount for an account within a time window. Used to enforce the ₹10,00,000 / day per-account cap.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    @GetMapping("/analytics/total-amount")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalTransactedAmount(
            @RequestParam @NotBlank(message = "Account ID is required") String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        log.debug("GET /api/v1/transactions/analytics/total-amount | accountId={}", accountId);
        BigDecimal total = transactionService.getTotalTransactedAmount(accountId, from, to);
        return ResponseEntity.ok(ApiResponse.success(total,
                "Total transacted amount fetched successfully"));
    }
    @Operation(
            summary = "Get transaction velocity count",
            description = """
                    Returns the count of transactions initiated by an account since a given datetime. Used to enforce the 50-transactions-per-day velocity cap and other fraud checks.

                    **Allowed roles:** CUSTOMER, CSR, BRANCH_MANAGER, LOAN_OFFICER, ADMIN"""
    )
    @GetMapping("/analytics/count")
    public ResponseEntity<ApiResponse<Long>> getTransactionCount(
            @RequestParam @NotBlank(message = "Account ID is required") String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        log.debug("GET /api/v1/transactions/analytics/count | accountId={} since={}", accountId, since);
        long count = transactionService.getTransactionCountSince(accountId, since);
        return ResponseEntity.ok(ApiResponse.success(count,
                "Transaction count fetched successfully"));
    }
    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0) throw new BadRequestException("Page index must not be negative.");
        if (size < 1) throw new BadRequestException("Page size must be at least 1.");
        int cappedSize = Math.min(size, 100);   // hard cap — never allow unbounded fetches
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, cappedSize, sort);
    }
}

