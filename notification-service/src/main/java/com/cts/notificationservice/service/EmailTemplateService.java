package com.cts.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine emailTemplateEngine;

    /**
     * Render a named template with variables.
     * Template file: classpath:templates/email/{templateId}.html
     */
    public String render(String templateId, Map<String, Object> variables) {
        try {
            Context ctx = new Context();
            if (variables != null) ctx.setVariables(variables);
            return emailTemplateEngine.process(templateId, ctx);
        } catch (Exception e) {
            log.warn("Failed to render template '{}', falling back to generic: {}", templateId, e.getMessage());
            return renderGeneric(
                    (String) (variables != null ? variables.getOrDefault("subject", "Notification") : "Notification"),
                    (String) (variables != null ? variables.getOrDefault("body", "") : ""),
                    variables);
        }
    }

    /**
     * Render the generic template for raw subject/body messages.
     * Used when no templateId is provided (e.g. existing service stubs).
     */
    public String renderGeneric(String subject, String body, Map<String, Object> extra) {
        Context ctx = new Context();
        ctx.setVariable("subject", subject != null ? subject : "Notification from BankSphere");
        ctx.setVariable("body", body != null ? body : "");
        if (extra != null) ctx.setVariables(extra);
        return emailTemplateEngine.process("generic", ctx);
    }

    // ── SMS templates — plain text, 160-char limit ────────────────────────────

    private static final Map<String, String> SMS_TEMPLATES = Map.ofEntries(
        Map.entry("account-application-submitted",
                "BankSphere: Your {accountType} application (Ref: {appRef}) has been received. We'll notify you within 2 business days."),
        Map.entry("account-application-approved",
                "BankSphere: Your {accountType} account is APPROVED! Account No: {accountNo}. Visit your branch to activate."),
        Map.entry("account-application-rejected",
                "BankSphere: Your {accountType} account application was not approved. Reason: {reason}. Visit branch for details."),
        Map.entry("account-frozen",
                "BankSphere ALERT: Your account *{lastFour} has been frozen. If unexpected, call 1800-XXX-XXXX immediately."),
        Map.entry("account-unfrozen",
                "BankSphere: Your account *{lastFour} has been unfrozen and is active."),
        Map.entry("account-closed",
                "BankSphere: Your account *{lastFour} has been closed as requested."),
        Map.entry("transaction-success",
                "BankSphere: Txn of Rs.{amount} on {accountNo}. Available Bal: Rs.{balance}. Ref:{txnId}. Not you? Call 1800-XXX-XXXX."),
        Map.entry("loan-approved",
                "BankSphere: CONGRATULATIONS! Your loan of Rs.{loanAmount} is approved. Loan ID: {loanId}. Disbursement within 24 hrs."),
        Map.entry("loan-rejected",
                "BankSphere: Your loan application {loanId} was not approved. For details, visit your nearest branch."),
        Map.entry("password-reset",
                "BankSphere: Your OTP for password reset is {otp}. Valid for 10 minutes. DO NOT share this OTP with anyone."),
        Map.entry("emi-reminder",
                "BankSphere: EMI of Rs.{emiAmount} for loan {loanId} is due on {dueDate}. Ensure sufficient balance to avoid penalty.")
    );

    public String renderSms(String templateId, Map<String, Object> variables) {
        String template = SMS_TEMPLATES.getOrDefault(templateId,
                "BankSphere: {subject}");

        if (variables == null) return template;

        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }
}
