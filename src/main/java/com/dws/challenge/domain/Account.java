package com.dws.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class Account {

    @NotEmpty(message = "Account ID must not be empty.")
    @Schema(description = "Unique identifier of the account", example = "Id-123", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String accountId;

    @NotNull(message = "Initial balance must be specified.")
    @PositiveOrZero(message = "Initial balance must be zero or positive.")
    @Schema(description = "Account balance", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal balance;

    // Granular instance lock ensuring thread safety across multi-account transfers
    @JsonIgnore
    private final transient ReentrantLock accountLock = new ReentrantLock(true);

    public Account(String accountId) {
        this.accountId = accountId;
        this.balance = BigDecimal.ZERO;
    }

    @JsonCreator
    public Account(@JsonProperty("accountId") String accountId,
                   @JsonProperty("balance") BigDecimal balance) {
        this.accountId = accountId;
        this.balance = balance;
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}