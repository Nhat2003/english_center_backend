package com.example.English.Center.Data.entity.payments;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderRef; // internal order ref

    @Column(nullable = false)
    private Long studentId;

    // Link payment to a class_room (nullable)
    @Column(name = "class_room_id")
    private Long classRoomId;

    @Column(nullable = false)
    private Long amount; // store in VND (e.g., 10000)

    // Persist currency column (DB has this column in your screenshot)
    @Column(name = "currency")
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    private String vnpTxnRef; // vnp_TxnRef

    private String vnpResponseCode;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    // new flag to indicate whether payment was successful and persisted as paid
    @Column(name = "paid", nullable = false)
    private Boolean paid = Boolean.FALSE;

    // Due date for this payment (Sunday of first week)
    @Column(name = "due_date")
    private LocalDate dueDate;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderRef() {
        return orderRef;
    }

    public void setOrderRef(String orderRef) {
        this.orderRef = orderRef;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getClassRoomId() {
        return classRoomId;
    }

    public void setClassRoomId(Long classRoomId) {
        this.classRoomId = classRoomId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getVnpTxnRef() {
        return vnpTxnRef;
    }

    public void setVnpTxnRef(String vnpTxnRef) {
        this.vnpTxnRef = vnpTxnRef;
    }

    public String getVnpResponseCode() {
        return vnpResponseCode;
    }

    public void setVnpResponseCode(String vnpResponseCode) {
        this.vnpResponseCode = vnpResponseCode;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
