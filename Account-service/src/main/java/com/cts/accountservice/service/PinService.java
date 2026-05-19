package com.cts.accountservice.service;

import com.cts.accountservice.entity.Account;
import com.cts.accountservice.exception.InvalidOperationException;
import com.cts.accountservice.exception.ResourceNotFoundException;
import com.cts.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PinService {

    private static final Pattern PIN_PATTERN  = Pattern.compile("^\\d{4,6}$");
    private static final int     MAX_ATTEMPTS = 5;
    private static final int     LOCK_MINUTES = 15;

    private final AccountRepository accountRepository;
    private final PasswordEncoder    pinEncoder;

    @Transactional
    public void setInitialPin(String accountNo, String customerId, String newPin) {
        validatePinFormat(newPin);
        Account account = mustOwn(accountNo, customerId);

        if (account.getTransactionPin() != null) {
            throw new InvalidOperationException(
                    "PIN already set for this account. Use change-pin to update it.");
        }
        account.setTransactionPin(pinEncoder.encode(newPin));
        account.setPinSetAt(LocalDateTime.now());
        account.setPinFailedAttempts(0);
        account.setPinLockedUntil(null);
        accountRepository.save(account);
        log.info("PIN set for account {}", accountNo);
    }

    @Transactional
    public void changePin(String accountNo, String customerId, String currentPin, String newPin) {
        validatePinFormat(newPin);
        Account account = mustOwn(accountNo, customerId);

        if (account.getTransactionPin() == null) {
            throw new InvalidOperationException("No PIN is set on this account yet. Use set-pin first.");
        }
        verifyPinOrFail(account, currentPin);
        if (pinEncoder.matches(newPin, account.getTransactionPin())) {
            throw new InvalidOperationException("New PIN must be different from the current PIN.");
        }
        account.setTransactionPin(pinEncoder.encode(newPin));
        account.setPinSetAt(LocalDateTime.now());
        account.setPinFailedAttempts(0);
        account.setPinLockedUntil(null);
        accountRepository.save(account);
        log.info("PIN changed for account {}", accountNo);
    }

    /**
     * Verifies the supplied PIN matches the stored hash and the account isn't
     * locked. Increments the failed-attempt counter on mismatch and locks the
     * PIN for LOCK_MINUTES once MAX_ATTEMPTS are exhausted. Resets the counter
     * on success. Used by the transfer flow.
     */
    @Transactional
    public void verifyPinOrFail(Account account, String pin) {
        ensureNotLocked(account);

        if (account.getTransactionPin() == null) {
            throw new InvalidOperationException(
                    "No transaction PIN is set on this account. Please set a PIN before transacting.");
        }
        if (!pinEncoder.matches(pin, account.getTransactionPin())) {
            accountRepository.incrementPinAttempts(account.getAccountNo());
            int attempts = (account.getPinFailedAttempts() == null ? 0 : account.getPinFailedAttempts()) + 1;
            if (attempts >= MAX_ATTEMPTS) {
                accountRepository.lockPinUntil(account.getAccountNo(),
                        LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                throw new InvalidOperationException(
                        "Too many incorrect PIN attempts. PIN locked for " + LOCK_MINUTES + " minutes.");
            }
            throw new InvalidOperationException(
                    "Incorrect PIN. " + (MAX_ATTEMPTS - attempts) + " attempt(s) remaining.");
        }
        // Success path — reset the counter
        accountRepository.resetPinAttempts(account.getAccountNo());
    }

    public boolean isPinSet(String accountNo, String customerId) {
        return accountRepository.findActiveOwnedAccount(accountNo, customerId)
                .map(a -> a.getTransactionPin() != null)
                .orElse(false);
    }

    /**
     * Verifies a PIN for an active account looked up by accountNo. Used by
     * inter-service calls (loan-service EMI / prepay) where the caller doesn't
     * have a customerId in scope but does pass through the gateway-validated
     * PIN. Reuses the same lockout + attempt-counter semantics as the transfer
     * flow.
     */
    @Transactional
    public void verifyPinForAccount(String accountNo, String pin) {
        Account account = accountRepository.findActiveByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active account " + accountNo));
        verifyPinOrFail(account, pin);
    }

    private Account mustOwn(String accountNo, String customerId) {
        return accountRepository.findActiveOwnedAccount(accountNo, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active account " + accountNo + " for the current user."));
    }

    private static void validatePinFormat(String pin) {
        if (pin == null || !PIN_PATTERN.matcher(pin).matches()) {
            throw new InvalidOperationException("PIN must be 4–6 digits.");
        }
    }

    private static void ensureNotLocked(Account a) {
        if (a.getPinLockedUntil() != null && a.getPinLockedUntil().isAfter(LocalDateTime.now())) {
            throw new InvalidOperationException(
                    "PIN is locked until " + a.getPinLockedUntil() + " due to too many failed attempts.");
        }
    }
}
