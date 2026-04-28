package com.cts.loanservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EmiScheduleResponse {

    private Double emi;
    private List<Double> schedule;
}
