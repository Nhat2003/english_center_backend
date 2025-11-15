package com.example.English.Center.Data.service;

import com.example.English.Center.Data.entity.payments.Payment;

import java.util.Map;
import java.util.Optional;

public interface PaymentService {
    Payment createPayment(Long studentId, Long amount, String description);
    String buildVnPayUrl(Payment payment);
    Optional<Payment> findByOrderRef(String orderRef);
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
    Optional<Payment> findById(Long id);
    boolean verifyAndUpdateFromVnPay(Map<String, String> params);
}
