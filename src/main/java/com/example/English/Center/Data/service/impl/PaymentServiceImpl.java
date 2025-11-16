package com.example.English.Center.Data.service.impl;

import com.example.English.Center.Data.config.VNPAYConfig;
import com.example.English.Center.Data.entity.payments.Payment;
import com.example.English.Center.Data.entity.payments.PaymentStatus;
import com.example.English.Center.Data.repository.payments.PaymentRepository;
import com.example.English.Center.Data.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public Payment createPayment(Long studentId, Long amount, String description) {
        // Validate amount and student ownership outside (controller)
        Payment payment = new Payment();
        payment.setOrderRef(UUID.randomUUID().toString().replace("-",""));
        payment.setStudentId(studentId);
        payment.setAmount(amount);
        payment.setCurrency("VND");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        return payment;
    }

    @Override
    public String buildVnPayUrl(Payment payment) {
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String orderType = "other";
            long amount = payment.getAmount() * 100L; // VNPAY expects amount * 100
            String bankCode = "NCB";

            String vnp_TxnRef = VNPAYConfig.getRandomNumber(8);
            String vnp_IpAddr = "127.0.0.1";

            String vnp_TmnCode = VNPAYConfig.vnp_TmnCode;

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");

            if (bankCode != null && !bankCode.isEmpty()) {
                vnp_Params.put("vnp_BankCode", bankCode);
            }
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan hoc phi:" + payment.getOrderRef());
            vnp_Params.put("vnp_OrderType", orderType);
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", VNPAYConfig.vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            List fieldNames = new ArrayList(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = (String) itr.next();
                String fieldValue = (String) vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    //Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    //Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }
            String queryUrl = query.toString();
            String vnp_SecureHash = VNPAYConfig.hmacSHA512(VNPAYConfig.vnp_HashSecret, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            String paymentUrl = VNPAYConfig.vnp_Url + "?" + queryUrl;

            // Save vnpTxnRef back to payment for later matching
            payment.setVnpTxnRef(vnp_TxnRef);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            return paymentUrl;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    public Optional<Payment> findByOrderRef(String orderRef) {
        return paymentRepository.findByOrderRef(orderRef);
    }

    @Override
    public Optional<Payment> findByVnpTxnRef(String vnpTxnRef) {
        return paymentRepository.findByVnpTxnRef(vnpTxnRef);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }

    @Override
    @Transactional
    public boolean verifyAndUpdateFromVnPay(Map<String, String> params) {
        // params should include vnp_SecureHash and vnp_TxnRef (vnp_TxnRef is ours), vnp_ResponseCode
        String secureHash = params.get("vnp_SecureHash");
        // Build hashData from params excluding vnp_SecureHash
        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getKey().startsWith("vnp_") && !"vnp_SecureHash".equals(e.getKey())) {
                fields.put(e.getKey(), e.getValue());
            }
        }
        String hashData = VNPAYConfig.hashAllFields(fields);
        if (!hashData.equals(secureHash)) {
            return false;
        }
        String txnRef = params.get("vnp_TxnRef");
        Optional<Payment> opt = findByVnpTxnRef(txnRef);
        if (!opt.isPresent()) {
            return false;
        }
        Payment payment = opt.get();
        String responseCode = params.get("vnp_ResponseCode");
        payment.setRawResponse(params.toString());
        payment.setVnpResponseCode(responseCode);
        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        return true;
    }
}
