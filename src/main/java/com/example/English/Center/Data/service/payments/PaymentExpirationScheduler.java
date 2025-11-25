package com.example.English.Center.Data.service.payments;

import com.example.English.Center.Data.entity.payments.Payment;
import com.example.English.Center.Data.entity.payments.PaymentStatus;
import com.example.English.Center.Data.repository.payments.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentExpirationScheduler {
    private static final Logger log = LoggerFactory.getLogger(PaymentExpirationScheduler.class);

    private final PaymentRepository paymentRepository;

    // Default expiration: 15 minutes
    private static final long EXP_MINUTES = 15L;

    public PaymentExpirationScheduler(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // Run every minute
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expirePendingPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(EXP_MINUTES);
        List<Payment> oldPendings = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, threshold);
        if (oldPendings == null || oldPendings.isEmpty()) return;
        for (Payment p : oldPendings) {
            p.setStatus(PaymentStatus.EXPIRED);
            p.setPaid(Boolean.FALSE);
            p.setUpdatedAt(LocalDateTime.now());
        }
        paymentRepository.saveAll(oldPendings);
        log.info("Expired {} pending payments older than {} minutes", oldPendings.size(), EXP_MINUTES);
    }
}

