package com.cts.identityservices.dto;

import com.cts.identityservices.entity.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffStatusRequest {

    @NotNull(message = "Status is required")
    private Status status;
}
