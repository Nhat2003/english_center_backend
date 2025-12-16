package com.example.English.Center.Data.dto.payments;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ManualPaymentDto {
    @NotNull(message = "studentId is required")
    private Long studentId;

    // amount in VND, optional (if not provided, system owner should fill or set to 0)
    @PositiveOrZero(message = "amount must be >= 0")
    private Long amount;

    // optional class room id
    private Long classRoomId;

    // status to set (default SUCCESS) - allowed: PENDING, SUCCESS, FAILED, EXPIRED, CANCELED
    private String status;

    // optional note (e.g., "paid in cash")
    private String note;
}

