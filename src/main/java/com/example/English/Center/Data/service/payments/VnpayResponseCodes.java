package com.example.English.Center.Data.service.payments;

import java.util.HashMap;
import java.util.Map;

/**
 * Lookup for VNPAY response codes -> human readable Vietnamese messages.
 * Keep this mapping in sync with the gateway docs; unknown codes return a sensible default.
 */
public final class VnpayResponseCodes {

    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("00", "Thanh toán học phí thanh công");
        MESSAGES.put("07", "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường). ");
        MESSAGES.put("09", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.");
        MESSAGES.put("10", "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần");
        MESSAGES.put("11", "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.");
        MESSAGES.put("12", "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.");
        MESSAGES.put("13", "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP). Xin quý khách vui lòng thực hiện lại giao dịch.");
        MESSAGES.put("24", "Giao dịch không thành công do: Khách hàng hủy giao dịch");
        MESSAGES.put("51", "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.");
        MESSAGES.put("65", "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.");
        MESSAGES.put("75", "Ngân hàng thanh toán đang bảo trì.");
        MESSAGES.put("79", "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định. Xin quý khách vui lòng thực hiện lại giao dịch");
        MESSAGES.put("99", "Lỗi hệ thống. Vui lòng thử lại sau.");
        // You can add more codes here as needed
    }

    private VnpayResponseCodes() {}

    public static String getMessage(String code) {
        if (code == null) return null;
        String msg = MESSAGES.get(code);
        if (msg != null) return msg;
        return "Mã trả về không xác định: " + code;
    }
}

