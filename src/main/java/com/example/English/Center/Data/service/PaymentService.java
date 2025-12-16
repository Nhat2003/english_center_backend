package com.example.English.Center.Data.service;

import com.example.English.Center.Data.dto.payments.ManualPaymentDto;
import com.example.English.Center.Data.entity.payments.Payment;
import com.example.English.Center.Data.entity.payments.PaymentStatus;

import java.util.Map;
import java.util.Optional;

public interface PaymentService {
    Payment createPayment(Long studentId, Long amount, String description);
    String buildVnPayUrl(Payment payment);
    Optional<Payment> findByOrderRef(String orderRef);
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
    Optional<Payment> findById(Long id);
    boolean verifyAndUpdateFromVnPay(Map<String, String> params);

    // Admin: update payment status manually (idempotent)
    void updatePaymentStatus(Long paymentId, PaymentStatus newStatus);

    // Admin: create a manual payment (cash) and mark according to provided status
    Payment createManualPayment(ManualPaymentDto dto);

    // Admin convenience: mark student as paid (try update existing pending payment in class; otherwise create manual payment)
    Payment createOrMarkPaidByStudent(Long studentId, ManualPaymentDto dto);
}
