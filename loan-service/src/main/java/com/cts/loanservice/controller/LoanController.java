package com.cts.loanservice.controller;

import com.cts.loanservice.dto.request.EmiPaymentRequest;
import com.cts.loanservice.dto.request.LoanApplyRequest;
import com.cts.loanservice.dto.request.LoanDecisionRequest;
import com.cts.loanservice.dto.response.EmiScheduleResponse;
import com.cts.loanservice.dto.response.LoanResponse;
import com.cts.loanservice.dto.response.LoanSummaryResponse;
import com.cts.loanservice.service.LoanService;
import com.cts.loanservice.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService service;

    @PostMapping
    public ResponseEntity<ApiResponse<LoanResponse>> apply(@RequestBody LoanApplyRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.applyLoan(req)));
    }

    @PostMapping("/{id}/decision")
    public ResponseEntity<ApiResponse<LoanResponse>> decide(@PathVariable String id,
                                                            @RequestBody LoanDecisionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.decideLoan(id, req)));
    }

    @PostMapping("/{id}/disburse")
    public ResponseEntity<ApiResponse<LoanResponse>> disburse(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.disburse(id)));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<LoanResponse>> pay(@PathVariable String id,
                                                         @RequestBody EmiPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.payEmi(id, req)));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<EmiScheduleResponse>> schedule(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getSchedule(id)));
    }

    @GetMapping("/summary/{customerId}")
    public ResponseEntity<ApiResponse<LoanSummaryResponse>> summary(@PathVariable String customerId) {
        return ResponseEntity.ok(ApiResponse.success(service.getSummary(customerId)));
    }
}