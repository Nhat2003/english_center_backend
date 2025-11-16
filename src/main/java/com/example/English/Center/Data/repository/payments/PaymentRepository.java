package com.example.English.Center.Data.repository.payments;

import com.example.English.Center.Data.entity.payments.Payment;
import com.example.English.Center.Data.entity.payments.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderRef(String orderRef);
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
    // Find payments for a student (recent first)
    List<Payment> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    // find payments for student & class with a given status
    List<Payment> findByStudentIdAndClassRoomIdAndStatus(Long studentId, Long classRoomId, com.example.English.Center.Data.entity.payments.PaymentStatus status);
    List<Payment> findByStudentIdAndClassRoomId(Long studentId, Long classRoomId);

    // Admin: list payments of a class (recent first)
    List<Payment> findByClassRoomIdOrderByCreatedAtDesc(Long classRoomId);
    // Admin: optionally filter by status if needed later
    List<Payment> findByClassRoomIdAndStatusOrderByCreatedAtDesc(Long classRoomId, PaymentStatus status);

    // Scheduler: pending payments older than threshold
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime before);
}
