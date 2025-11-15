package com.example.English.Center.Data.controller.payments;

import com.example.English.Center.Data.config.VNPAYConfig;
import com.example.English.Center.Data.entity.payments.Payment;
import com.example.English.Center.Data.entity.payments.PaymentStatus;
import com.example.English.Center.Data.repository.payments.PaymentRepository;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final ClassEntityRepository classEntityRepository;
    private final PaymentRepository paymentRepository;

    public PaymentController(ClassEntityRepository classEntityRepository, PaymentRepository paymentRepository) {
        this.classEntityRepository = classEntityRepository;
        this.paymentRepository = paymentRepository;
    }

    // DTO trả về URL và trạng thái
    public static class PaymentUrlResponse {
        private String status;
        private String url;
        private Long paymentId;

        public PaymentUrlResponse() {}

        public PaymentUrlResponse(String status, String url) {
            this.status = status;
            this.url = url;
        }

        public PaymentUrlResponse(String status, Long paymentId, String url) {
            this.status = status;
            this.paymentId = paymentId;
            this.url = url;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Long getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(Long paymentId) {
            this.paymentId = paymentId;
        }
    }

    @GetMapping("/create-url")
    public ResponseEntity<PaymentUrlResponse> createPayment(@RequestParam Long classRoomId) throws UnsupportedEncodingException {
        // Lấy thông tin lớp và khóa học để lấy đúng học phí
        var classOpt = classEntityRepository.findById(classRoomId);
        if (classOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new PaymentUrlResponse("error", "ClassRoom not found"));
        }
        var classRoom = classOpt.get();
        var course = classRoom.getCourse();
        if (course == null || course.getFee() == null) {
            return ResponseEntity.status(500).body(new PaymentUrlResponse("error", "Course fee not configured"));
        }
        BigDecimal fee = course.getFee();
        long amountVnd = fee.longValue(); // assume fee stored in VND

        // Optional: nếu có auth, kiểm tra student thuộc lớp
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            try {
                Long studentId = Long.parseLong(auth.getName());
                boolean isMember = classRoom.getStudents() != null && classRoom.getStudents().stream().anyMatch(s -> s.getId().equals(studentId));
                if (!isMember) {
                    return ResponseEntity.status(403).body(new PaymentUrlResponse("error", "Student not member of class"));
                }
            } catch (Exception e) {
                // không ép buộc, bỏ qua nếu principal không phải id
            }
        }

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        long amount = amountVnd * 100L; // VNPAY expects amount * 100
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
        vnp_Params.put("vnp_OrderInfo", "Thanh toan hoc phi cho lop:" + classRoomId + " ref:" + vnp_TxnRef);
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

        // Persist Payment record so we can track status later
        // Persist Payment first so we can include paymentId in vnp_ReturnUrl
        Payment payment = new Payment();
        payment.setOrderRef(UUID.randomUUID().toString().replace("-",""));
        // set studentId when authenticated and parseable
        if (auth != null && auth.isAuthenticated()) {
            try {
                Long studentId = Long.parseLong(auth.getName());
                payment.setStudentId(studentId);
            } catch (Exception ignore) {}
        }
        payment.setAmount(Long.valueOf(amountVnd));
        payment.setCurrency("VND");
        payment.setVnpTxnRef(vnp_TxnRef);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(java.time.LocalDateTime.now());
        payment.setUpdatedAt(java.time.LocalDateTime.now());
        payment = paymentRepository.save(payment);

        // include paymentId in return url so frontend can map user after redirect
        String returnUrlWithPayment = VNPAYConfig.vnp_ReturnUrl + (VNPAYConfig.vnp_ReturnUrl.contains("?") ? "&" : "?") + "paymentId=" + payment.getId();
        // replace vnp_ReturnUrl param used earlier
        vnp_Params.put("vnp_ReturnUrl", returnUrlWithPayment);

        // rebuild query and secure hash because vnp_ReturnUrl changed
        fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        hashData = new StringBuilder();
        query = new StringBuilder();
        itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) { query.append('&'); hashData.append('&'); }
            }
        }
        queryUrl = query.toString();
        vnp_SecureHash = VNPAYConfig.hmacSHA512(VNPAYConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        paymentUrl = VNPAYConfig.vnp_Url + "?" + queryUrl;

        PaymentUrlResponse resp = new PaymentUrlResponse("success", payment.getId(), paymentUrl);
        return ResponseEntity.ok(resp);
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable Long id) {
        Optional<Payment> opt = paymentRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Payment p = opt.get();
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", p.getId());
        resp.put("orderRef", p.getOrderRef());
        resp.put("studentId", p.getStudentId());
        resp.put("amount", p.getAmount());
        resp.put("currency", p.getCurrency());
        resp.put("status", p.getStatus() != null ? p.getStatus().name() : null);
        resp.put("vnpTxnRef", p.getVnpTxnRef());
        resp.put("vnpResponseCode", p.getVnpResponseCode());
        resp.put("createdAt", p.getCreatedAt());
        resp.put("updatedAt", p.getUpdatedAt());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/txn/{txnRef}")
    public ResponseEntity<?> getPaymentByTxn(@PathVariable String txnRef) {
        Optional<Payment> opt = paymentRepository.findByVnpTxnRef(txnRef);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Payment p = opt.get();
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", p.getId());
        resp.put("orderRef", p.getOrderRef());
        resp.put("studentId", p.getStudentId());
        resp.put("amount", p.getAmount());
        resp.put("currency", p.getCurrency());
        resp.put("status", p.getStatus() != null ? p.getStatus().name() : null);
        resp.put("vnpTxnRef", p.getVnpTxnRef());
        resp.put("vnpResponseCode", p.getVnpResponseCode());
        resp.put("createdAt", p.getCreatedAt());
        resp.put("updatedAt", p.getUpdatedAt());
        return ResponseEntity.ok(resp);
    }

    // Trả về số tiền cần nạp cho một lớp (lấy từ Course.fee)
    @GetMapping("/due")
    public ResponseEntity<?> getDueAmount(@RequestParam Long classRoomId) {
        var classOpt = classEntityRepository.findById(classRoomId);
        if (classOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "ClassRoom not found"));
        }
        var classRoom = classOpt.get();
        var course = classRoom.getCourse();
        if (course == null || course.getFee() == null) {
            return ResponseEntity.status(500).body(Map.of("error", "Course fee not configured"));
        }
        BigDecimal fee = course.getFee();
        long amountVnd = fee.longValue();
        Map<String, Object> resp = new HashMap<>();
        resp.put("amount", amountVnd);
        resp.put("currency", "VND");
        resp.put("message", "Amount due for class " + classRoomId);
        return ResponseEntity.ok(resp);
    }

    // IPN endpoint: VNPAY gọi server-to-server để thông báo kết quả giao dịch
    // Phải kiểm tra checksum trước, sau đó kiểm tra order tồn tại, amount, status và cập nhật DB
    @PostMapping(path = "/ipn", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Map<String, String>> ipn(@RequestParam Map<String, String> params) {
        try {
            // Collect vnp_ params except secure hash
            Map<String, String> fields = new HashMap<>();
            for (Map.Entry<String, String> e : params.entrySet()) {
                String k = e.getKey();
                String v = e.getValue();
                if (k == null) continue;
                if (k.equals("vnp_SecureHash") || k.equals("vnp_SecureHashType")) continue;
                if (k.startsWith("vnp_")) {
                    fields.put(k, v);
                }
            }

            String vnpSecureHash = params.get("vnp_SecureHash");
            String signValue = VNPAYConfig.hashAllFields(fields);
            if (signValue == null || !signValue.equals(vnpSecureHash)) {
                return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
            }

            String txnRef = params.get("vnp_TxnRef");
            if (txnRef == null) {
                return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not Found"));
            }

            Optional<com.example.English.Center.Data.entity.payments.Payment> opt = paymentRepository.findByVnpTxnRef(txnRef);
            if (opt.isEmpty()) {
                return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not Found"));
            }
            com.example.English.Center.Data.entity.payments.Payment payment = opt.get();

            // Check amount: vnp_Amount is sent multiplied by 100
            String vnpAmountStr = params.get("vnp_Amount");
            long vnpAmount = 0L;
            try {
                vnpAmount = Long.parseLong(vnpAmountStr);
            } catch (Exception ex) {
                return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid Amount"));
            }
            long expected = (payment.getAmount() == null ? 0L : payment.getAmount() * 100L);
            if (vnpAmount != expected) {
                return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid Amount"));
            }

            // Check order status
            if (payment.getStatus() != null && payment.getStatus() != PaymentStatus.PENDING) {
                return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
            }

            // Update payment based on response
            String vnpResponseCode = params.get("vnp_ResponseCode");
            String vnpTransactionStatus = params.get("vnp_TransactionStatus");
            payment.setVnpResponseCode(vnpResponseCode);
            payment.setRawResponse(params.toString());
            if ("00".equals(vnpResponseCode) || "00".equals(vnpTransactionStatus)) {
                payment.setStatus(PaymentStatus.SUCCESS);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
            payment.setUpdatedAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));

        } catch (Exception ex) {
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Unknow error"));
        }
    }

    // Trả về số tiền các lớp mà một học sinh cần nạp
    @GetMapping("/due-for-student")
    public ResponseEntity<?> getDueForStudent(@RequestParam Long studentId) {
        if (studentId == null) return ResponseEntity.badRequest().body(Map.of("error", "studentId required"));
        List<com.example.English.Center.Data.entity.classes.ClassRoom> classes = classEntityRepository.findByStudents_Id(studentId);
        if (classes == null || classes.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (var cls : classes) {
            var course = cls.getCourse();
            long amountVnd = 0L;
            if (course != null && course.getFee() != null) {
                amountVnd = course.getFee().longValue();
            }
            Map<String,Object> item = new HashMap<>();
            item.put("classRoomId", cls.getId());
            item.put("className", cls.getName());
            item.put("amount", amountVnd);
            item.put("currency", "VND");
            list.add(item);
        }
        return ResponseEntity.ok(list);
    }

    // Trả về tổng quan thanh toán cho học sinh: danh sách lớp + số tiền cần đóng và lịch sử giao dịch
    @GetMapping("/student/{studentId}/overview")
    public ResponseEntity<?> getStudentPaymentOverview(@PathVariable Long studentId) {
        if (studentId == null) return ResponseEntity.badRequest().body(Map.of("error", "studentId required"));

        // 1) Danh sách lớp + due
        List<com.example.English.Center.Data.entity.classes.ClassRoom> classes = classEntityRepository.findByStudents_Id(studentId);
        List<Map<String,Object>> dues = new ArrayList<>();
        if (classes != null) {
            for (var cls : classes) {
                var course = cls.getCourse();
                long amountVnd = 0L;
                if (course != null && course.getFee() != null) amountVnd = course.getFee().longValue();
                Map<String,Object> item = new HashMap<>();
                item.put("classRoomId", cls.getId());
                item.put("className", cls.getName());
                item.put("amount", amountVnd);
                item.put("currency", "VND");
                dues.add(item);
            }
        }

        // 2) Lịch sử thanh toán (gần nhất trước)
        List<Payment> payments = paymentRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        List<Map<String,Object>> history = new ArrayList<>();
        if (payments != null) {
            for (var p : payments) {
                Map<String,Object> m = new HashMap<>();
                m.put("id", p.getId());
                m.put("orderRef", p.getOrderRef());
                m.put("amount", p.getAmount());
                m.put("currency", p.getCurrency());
                m.put("status", p.getStatus() != null ? p.getStatus().name() : null);
                m.put("vnpTxnRef", p.getVnpTxnRef());
                m.put("vnpResponseCode", p.getVnpResponseCode());
                m.put("createdAt", p.getCreatedAt());
                m.put("updatedAt", p.getUpdatedAt());
                history.add(m);
            }
        }

        Map<String,Object> resp = new HashMap<>();
        resp.put("dues", dues);
        resp.put("payments", history);
        return ResponseEntity.ok(resp);
    }

    // Tạo URL thanh toán theo studentId (và optional classRoomId). Nếu student tham gia nhiều lớp và classRoomId không truyền, trả lỗi yêu cầu chỉ định.
    @GetMapping("/create-url-by-student")
    public ResponseEntity<?> createUrlByStudent(@RequestParam Long studentId, @RequestParam(required = false) Long classRoomId) throws UnsupportedEncodingException {
        if (studentId == null) return ResponseEntity.badRequest().body(Map.of("error","studentId required"));

        // basic auth check: nếu auth present và auth name có thể parse thành id khác studentId -> 403
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            try {
                Long authId = Long.parseLong(auth.getName());
                if (!authId.equals(studentId)) {
                    return ResponseEntity.status(403).body(Map.of("error","Forbidden: cannot create payment for other student"));
                }
            } catch (Exception ignored) {
                // nếu không parse được, không block (admins or other principals)
            }
        }

        com.example.English.Center.Data.entity.classes.ClassRoom targetClass = null;
        if (classRoomId != null) {
            var opt = classEntityRepository.findById(classRoomId);
            if (opt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","ClassRoom not found"));
            targetClass = opt.get();
            boolean member = targetClass.getStudents() != null && targetClass.getStudents().stream().anyMatch(s -> s.getId().equals(studentId));
            if (!member) return ResponseEntity.status(403).body(Map.of("error","Student not member of class"));
        } else {
            List<com.example.English.Center.Data.entity.classes.ClassRoom> classes = classEntityRepository.findByStudents_Id(studentId);
            if (classes == null || classes.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Student not enrolled in any class"));
            if (classes.size() > 1) {
                // ambiguous: require client to specify classRoomId
                List<Long> ids = classes.stream().map(com.example.English.Center.Data.entity.classes.ClassRoom::getId).toList();
                return ResponseEntity.badRequest().body(Map.of("error","Multiple classes found. Please specify classRoomId.", "classRoomIds", ids));
            }
            targetClass = classes.get(0);
        }

        // Reuse logic to build vnp params and persist payment (same as create-url)
        var course = targetClass.getCourse();
        if (course == null || course.getFee() == null) return ResponseEntity.status(500).body(Map.of("error","Course fee not configured"));
        long amountVnd = course.getFee().longValue();

        // Check if student already paid for this class (sum of SUCCESS payments >= fee)
        List<Payment> successPayments = paymentRepository.findByStudentIdAndClassRoomIdAndStatus(studentId, targetClass.getId(), PaymentStatus.SUCCESS);
        long paidAmount = 0L;
        if (successPayments != null) {
            for (var sp : successPayments) {
                if (sp.getAmount() != null) paidAmount += sp.getAmount();
            }
        }
        if (paidAmount >= amountVnd) {
            // Already paid in full - return history and a message
            List<Payment> payments = paymentRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
            Map<String,Object> resp = new HashMap<>();
            resp.put("message", "Already paid in full for this class");
            resp.put("paidAmount", paidAmount);
            resp.put("amount", amountVnd);
            resp.put("payments", payments);
            return ResponseEntity.ok(resp);
        }

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        long amount = amountVnd * 100L;
        String bankCode = "NCB";
        String vnp_TxnRef = VNPAYConfig.getRandomNumber(8);
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = VNPAYConfig.vnp_TmnCode;

        Map<String,String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan hoc phi cho student:"+studentId+" class:"+targetClass.getId()+" ref:"+vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPAYConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) { query.append('&'); hashData.append('&'); }
            }
        }
        String vnp_SecureHash = VNPAYConfig.hmacSHA512(VNPAYConfig.vnp_HashSecret, hashData.toString());
        String queryUrl = query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPAYConfig.vnp_Url + "?" + queryUrl;

        // persist payment
        try {
            Payment payment = new Payment();
            payment.setOrderRef(UUID.randomUUID().toString().replace("-",""));
            payment.setStudentId(studentId);
            payment.setClassRoomId(targetClass.getId());
            payment.setAmount(Long.valueOf(amountVnd));
            payment.setCurrency("VND");
            payment.setVnpTxnRef(vnp_TxnRef);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(java.time.LocalDateTime.now());
            payment.setUpdatedAt(java.time.LocalDateTime.now());
            payment = paymentRepository.save(payment);
            return ResponseEntity.ok(Map.of("status","success","paymentId", payment.getId(), "url", paymentUrl));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error","failed to persist payment", "url", paymentUrl));
        }
    }

}
