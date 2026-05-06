package com.cts.loanservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmiScheduleResponse {

    private Double emiAmount;
    private Integer totalMonths;
    private List<EmiDetail> schedule;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmiDetail {
        private int month;
        private Double emi;
        private Double principal;
        private Double interest;
        private Double balance;
    }
}
