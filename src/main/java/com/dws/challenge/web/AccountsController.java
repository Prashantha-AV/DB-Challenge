package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.ErrorResponse;
import com.dws.challenge.dto.TransferRequest;
import com.dws.challenge.service.AccountsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
@Tag(name = "Accounts Controller", description = "Endpoints for managing bank accounts and initiating fund transfers")
public class AccountsController {

    private final AccountsService accountsService;

    @Autowired
    public AccountsController(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    @Operation(summary = "Register a new account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or duplicate account ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> registerAccount(@RequestBody @Valid Account account) {
        log.info("Registering new account: {}", account.getAccountId());
        this.accountsService.createAccount(account);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Operation(summary = "Fetch account details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(path = "/{accountId}")
    public ResponseEntity<Account> fetchAccountById(@PathVariable String accountId) {
        log.info("Fetching details for account: {}", accountId);
        Account account = this.accountsService.getAccount(accountId);
        return ResponseEntity.ok(account);
    }

    @Operation(summary = "Execute fund transfer between accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer executed successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient funds, invalid amount, or same account transfer",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Source or destination account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(path = "/transfer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> executeTransfer(@RequestBody @Valid TransferRequest transferRequest) {
        log.info("Processing transfer request: {}", transferRequest);
        this.accountsService.transferFunds(transferRequest);
        return ResponseEntity.ok().build();
    }
}