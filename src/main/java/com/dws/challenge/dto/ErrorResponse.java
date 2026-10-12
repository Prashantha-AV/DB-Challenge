package com.dws.challenge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {

    @Schema(description = "Timestamp of the error", example = "2026-10-12T14:30:00")
    private final String timestamp;

    @Schema(description = "HTTP Status Code", example = "400")
    private final int status;

    @Schema(description = "HTTP Error Description", example = "Bad Request")
    private final String error;

    @Schema(description = "Detailed error message", example = "Debit and credit account identifiers must be distinct.")
    private final String message;

    public ErrorResponse(int status, String error, String message) {
        this.timestamp = LocalDateTime.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
    }
}