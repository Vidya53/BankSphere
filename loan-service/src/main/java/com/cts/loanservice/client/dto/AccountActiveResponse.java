package com.cts.loanservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Envelope for account-service's `GET /api/v1/internal/accounts/{accountNo}/active`.
 * The data field is a plain Boolean — true iff the account exists and is ACTIVE.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountActiveResponse {

    private Boolean data;
}
