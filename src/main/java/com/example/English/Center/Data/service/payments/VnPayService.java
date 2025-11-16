package com.example.English.Center.Data.service.payments;

import com.example.English.Center.Data.config.VNPAYConfig;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VnPayService {

    public String createPaymentUrl(Long amount, String orderInfo, String bankCode, String locale, String returnUrl, String orderId) {
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_TmnCode = VNPAYConfig.vnp_TmnCode;
            String vnp_ReturnUrl = (returnUrl != null && !returnUrl.isBlank()) ? returnUrl : VNPAYConfig.vnp_ReturnUrl;
            String vnp_Locale = (locale == null || locale.isBlank()) ? "vn" : locale;
            String vnp_CurrCode = "VND";

            String vnp_TxnRef = (orderId != null && !orderId.isBlank()) ? orderId : String.valueOf(System.currentTimeMillis());
            String vnp_OrderInfo = orderInfo != null ? orderInfo : "Payment";
            String vnp_OrderType = "other";
            String vnp_Amount = String.valueOf(amount * 100); // VNPAY expects amount in cents

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String vnp_CreateDate = now.format(formatter);

            Map<String, String> params = new HashMap<>();
            params.put("vnp_Version", vnp_Version);
            params.put("vnp_Command", vnp_Command);
            params.put("vnp_TmnCode", vnp_TmnCode);
            params.put("vnp_Amount", vnp_Amount);
            params.put("vnp_CurrCode", vnp_CurrCode);
            params.put("vnp_TxnRef", vnp_TxnRef);
            params.put("vnp_OrderInfo", vnp_OrderInfo);
            params.put("vnp_OrderType", vnp_OrderType);
            params.put("vnp_Locale", vnp_Locale);
            params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            params.put("vnp_CreateDate", vnp_CreateDate);
            if (bankCode != null && !bankCode.isBlank()) params.put("vnp_BankCode", bankCode);

            // sort by key
            List<String> keys = new ArrayList<>(params.keySet());
            Collections.sort(keys);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            for (String key : keys) {
                String value = params.get(key);
                if (value != null && value.length() > 0) {
                    // build hash data
                    hashData.append(key).append('=').append(value);
                    // build query
                    query.append(URLEncoder.encode(key, StandardCharsets.UTF_8.toString()))
                            .append('=')
                            .append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()));
                    if (!key.equals(keys.get(keys.size() - 1))) {
                        hashData.append('&');
                        query.append('&');
                    }
                }
            }

            String secureHash = VNPAYConfig.hmacSHA512(VNPAYConfig.vnp_HashSecret, hashData.toString());
            String paymentUrl = VNPAYConfig.vnp_Url + "?" + query.toString() + "&vnp_SecureHash=" + secureHash;
            return paymentUrl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}


