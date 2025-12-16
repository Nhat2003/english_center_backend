package com.example.English.Center.Data.dto.payments;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentStatusUpdateDto {
    @NotBlank(message = "Status is required")
    private String status; // Expected values: PENDING, SUCCESS, FAILED, EXPIRED, CANCELED
}

