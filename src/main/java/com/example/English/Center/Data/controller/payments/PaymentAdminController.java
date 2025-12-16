package com.example.English.Center.Data.controller.payments;

import com.example.English.Center.Data.dto.payments.ManualPaymentDto;
import com.example.English.Center.Data.dto.payments.PaymentStatusUpdateDto;
import com.example.English.Center.Data.entity.payments.Payment;
import com.example.English.Center.Data.entity.payments.PaymentStatus;
import com.example.English.Center.Data.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/payments")
public class PaymentAdminController {

    @Autowired
    private PaymentService paymentService;

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{paymentId}/status")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable Long paymentId, @RequestBody @Valid PaymentStatusUpdateDto dto) {
        try {
            PaymentStatus newStatus = PaymentStatus.valueOf(dto.getStatus().toUpperCase());
            paymentService.updatePaymentStatus(paymentId, newStatus);
            return ResponseEntity.ok(Map.of("message","Payment status updated", "paymentId", paymentId, "status", newStatus.name()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error","Invalid status value"));
        } catch (Exception ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/manual")
    public ResponseEntity<?> createManualPayment(@RequestBody @Valid ManualPaymentDto dto) {
        try {
            Payment created = paymentService.createManualPayment(dto);
            return ResponseEntity.ok(Map.of("message","Manual payment created", "paymentId", created.getId(), "status", created.getStatus().name()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/mark-paid")
    public ResponseEntity<?> markStudentPaid(@RequestBody @Valid ManualPaymentDto dto) {
        try {
            if (dto.getStudentId() == null) return ResponseEntity.badRequest().body(Map.of("error","studentId is required"));
            Payment payment = paymentService.createOrMarkPaidByStudent(dto.getStudentId(), dto);
            return ResponseEntity.ok(Map.of("message","Student payment recorded", "paymentId", payment.getId(), "status", payment.getStatus().name()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }
}
