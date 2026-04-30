package com.cts.loanservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EmiScheduleResponse {

    private Double emiAmount;
    private Integer totalMonths;
    private List<EmiDetail> schedule;

    @Data
    @Builder
    public static class EmiDetail {
        private int month;
        private Double emi;
        private Double principal;
        private Double interest;
        private Double balance;
    }
}
