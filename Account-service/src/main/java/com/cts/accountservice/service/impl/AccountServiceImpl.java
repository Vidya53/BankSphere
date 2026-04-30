package com.cts.accountservice.service.impl;

import com.cts.accountservice.dto.request.CloseAccountRequest;
import com.cts.accountservice.dto.request.FreezeAccountRequest;
import com.cts.accountservice.dto.response.AccountResponse;
import com.cts.accountservice.dto.response.BranchAccountSummary;
import com.cts.accountservice.entity.Account;
import com.cts.accountservice.enums.AccountStatus;
import com.cts.accountservice.enums.ApplicationStatus;
import com.cts.accountservice.exception.*;
import com.cts.accountservice.mapper.AccountMapper;
import com.cts.accountservice.repository.AccountApplicationRepository;
import com.cts.accountservice.repository.AccountRepository;
import com.cts.accountservice.security.UserContext;
import com.cts.accountservice.service.AccountService;
import com.cts.accountservice.service.AuditService;
import com.cts.accountservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountApplicationRepository applicationRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(UserContext userContext) {
        log.debug("Fetching accounts for customer {}", userContext.getUserId());
        return accountRepository.findByCustomerIdOrderByOpenedAtDesc(userContext.getUserId())
                .stream().map(AccountMapper::toAccountResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByAccountNo(String accountNo) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNo));
        return AccountMapper.toAccountResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getBranchAccounts(String branchCode, String status) {
        log.debug("Fetching accounts for branch {} with status filter: {}", branchCode, status);
        if (status != null && !status.isBlank()) {
            AccountStatus accountStatus = AccountStatus.valueOf(status.toUpperCase());
            return accountRepository.findByBranchCodeAndStatus(branchCode, accountStatus)
                    .stream().map(AccountMapper::toAccountResponse).toList();
        }
        return accountRepository.findByBranchCode(branchCode)
                .stream().map(AccountMapper::toAccountResponse).toList();
    }

    @Override
    public AccountResponse freezeAccount(String accountNo, FreezeAccountRequest request, UserContext staffContext) {
        log.info("Staff {} freezing account {}", staffContext.getUserId(), accountNo);

        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNo));

        if (!account.getBranchCode().equals(staffContext.getBranchCode())) {
            throw new UnauthorizedBranchAccessException("You can only freeze accounts within your branch");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidOperationException("Only ACTIVE accounts can be frozen. Current status: " + account.getStatus());
        }

        account.setStatus(AccountStatus.FROZEN);
        account.setFreezeReason(request.getReason());
        account.setFrozenBy(staffContext.getUserId());
        account.setFrozenAt(LocalDateTime.now());
        account.setIsTransactional(false);
        accountRepository.save(account);

        auditService.logAudit(staffContext.getUserId(), staffContext.getRole(), "ACCOUNT_FROZEN",
                "ACCOUNT", accountNo, account.getBranchCode());
        notificationService.sendNotification(account.getCustomerId(), account.getCustomerEmail(),
                "Account Frozen",
                "Your account ending with " + accountNo.substring(accountNo.length() - 4) + " has been frozen. Please contact your branch.");

        log.info("Account {} frozen by {}", accountNo, staffContext.getUserId());
        return AccountMapper.toAccountResponse(account);
    }

    @Override
    public AccountResponse unfreezeAccount(String accountNo, UserContext staffContext) {
        log.info("Staff {} unfreezing account {}", staffContext.getUserId(), accountNo);

        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNo));

        if (!account.getBranchCode().equals(staffContext.getBranchCode())) {
            throw new UnauthorizedBranchAccessException("You can only unfreeze accounts within your branch");
        }

        if (account.getStatus() != AccountStatus.FROZEN) {
            throw new InvalidOperationException("Only FROZEN accounts can be unfrozen. Current status: " + account.getStatus());
        }

        account.setStatus(AccountStatus.ACTIVE);
        account.setFreezeReason(null);
        account.setFrozenBy(null);
        account.setFrozenAt(null);
        account.setIsTransactional(true);
        accountRepository.save(account);

        auditService.logAudit(staffContext.getUserId(), staffContext.getRole(), "ACCOUNT_UNFROZEN",
                "ACCOUNT", accountNo, account.getBranchCode());
        notificationService.sendNotification(account.getCustomerId(), account.getCustomerEmail(),
                "Account Unfrozen",
                "Your account ending with " + accountNo.substring(accountNo.length() - 4) + " has been unfrozen and is now active.");

        log.info("Account {} unfrozen by {}", accountNo, staffContext.getUserId());
        return AccountMapper.toAccountResponse(account);
    }

    @Override
    public AccountResponse closeAccount(String accountNo, CloseAccountRequest request, UserContext staffContext) {
        log.info("Staff {} closing account {}", staffContext.getUserId(), accountNo);

        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNo));

        if (!account.getBranchCode().equals(staffContext.getBranchCode())) {
            throw new UnauthorizedBranchAccessException("You can only close accounts within your branch");
        }

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidOperationException("Account is already closed");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidOperationException(
                    "Account has remaining balance of ₹" + account.getBalance() + ". Please withdraw or transfer before closing.");
        }

        account.setStatus(AccountStatus.CLOSED);
        account.setCloseReason(request.getReason());
        account.setClosedBy(staffContext.getUserId());
        account.setClosedAt(LocalDateTime.now());
        account.setIsTransactional(false);
        accountRepository.save(account);

        auditService.logAudit(staffContext.getUserId(), staffContext.getRole(), "ACCOUNT_CLOSED",
                "ACCOUNT", accountNo, account.getBranchCode());
        notificationService.sendNotification(account.getCustomerId(), account.getCustomerEmail(),
                "Account Closed",
                "Your account ending with " + accountNo.substring(accountNo.length() - 4) + " has been closed.");

        log.info("Account {} closed by {}", accountNo, staffContext.getUserId());
        return AccountMapper.toAccountResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchAccountSummary getBranchSummary(String branchCode) {
        BigDecimal totalBalance = accountRepository.getTotalBalanceByBranch(branchCode);
        return BranchAccountSummary.builder()
                .branchCode(branchCode)
                .totalAccounts(accountRepository.findByBranchCode(branchCode).size())
                .activeAccounts(accountRepository.countByBranchCodeAndStatus(branchCode, AccountStatus.ACTIVE))
                .frozenAccounts(accountRepository.countByBranchCodeAndStatus(branchCode, AccountStatus.FROZEN))
                .closedAccounts(accountRepository.countByBranchCodeAndStatus(branchCode, AccountStatus.CLOSED))
                .pendingApplications(applicationRepository.countByBranchCodeAndStatus(branchCode, ApplicationStatus.SUBMITTED))
                .totalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO)
                .build();
    }

    // ---- Internal endpoints for transaction-service ----

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountActive(String accountNo) {
        return accountRepository.findByAccountNo(accountNo)
                .map(acc -> acc.getStatus() == AccountStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountNo) {
        Account account = accountRepository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNo));
        return account.getBalance();
    }

    @Override
    public boolean creditAccount(String accountNo, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Credit amount must be positive");
        }
        int rows = accountRepository.creditAccount(accountNo, amount);
        if (rows == 0) {
            throw new AccountNotActiveException("Account " + accountNo + " is not active or does not exist");
        }
        log.info("Credited ₹{} to account {}", amount, accountNo);
        return true;
    }

    @Override
    public boolean debitAccount(String accountNo, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Debit amount must be positive");
        }
        int rows = accountRepository.debitAccount(accountNo, amount);
        if (rows == 0) {
            Account account = accountRepository.findByAccountNo(accountNo)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNo));
            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new AccountNotActiveException("Account " + accountNo + " is not active");
            }
            throw new InsufficientBalanceException("Insufficient balance in account " + accountNo);
        }
        log.info("Debited ₹{} from account {}", amount, accountNo);
        return true;
    }
}

