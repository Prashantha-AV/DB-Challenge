package com.dws.challenge.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotEmpty(message = "Source account ID cannot be empty.")
    @Schema(description = "Account ID to transfer funds from", example = "Id-100", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String accountFromId;

    @NotEmpty(message = "Target account ID cannot be empty.")
    @Schema(description = "Account ID to receive funds", example = "Id-200", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String accountToId;

    @NotNull(message = "Transfer amount must be specified.")
    @Positive(message = "Amount must be greater than zero.")
    @Schema(description = "Transfer amount, must be positive", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private final BigDecimal amount;

    @JsonCreator
    public TransferRequest(@JsonProperty("accountFromId") String accountFromId,
                           @JsonProperty("accountToId") String accountToId,
                           @JsonProperty("amount") BigDecimal amount) {
        this.accountFromId = accountFromId;
        this.accountToId = accountToId;
        this.amount = amount;
    }
}