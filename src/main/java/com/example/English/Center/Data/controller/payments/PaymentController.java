package com.example.English.Center.Data.controller.payments;

import com.example.English.Center.Data.config.VNPAYConfig;
import com.example.English.Center.Data.entity.payments.Payment;
import com.example.English.Center.Data.entity.payments.PaymentStatus;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.payments.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final ClassEntityRepository classEntityRepository;
    private final PaymentRepository paymentRepository;

    public PaymentController(ClassEntityRepository classEntityRepository, PaymentRepository paymentRepository) {
        this.classEntityRepository = classEntityRepository;
        this.paymentRepository = paymentRepository;
    }

    // ---------------- DTO ----------------
    public static class PaymentUrlResponse {
        private String status;
        private String url;
        private Long paymentId;
        public PaymentUrlResponse() {}
        public PaymentUrlResponse(String status, Long paymentId, String url) { this.status = status; this.paymentId = paymentId; this.url = url; }
        public PaymentUrlResponse(String status, String url) { this.status = status; this.url = url; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Long getPaymentId() { return paymentId; }
        public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    }

    // ---------------- Helpers ----------------
    private String randomTxnRef() { return VNPAYConfig.getRandomNumber(8); }

    private Map<String,String> baseVnpParams(long amountVnd, String txnRef, String orderInfo) {
        long gatewayAmount = amountVnd * 100L; // multiply by 100 per VNPAY spec
        Map<String,String> m = new HashMap<>();
        m.put("vnp_Version","2.1.0");
        m.put("vnp_Command","pay");
        m.put("vnp_TmnCode", VNPAYConfig.vnp_TmnCode);
        m.put("vnp_Amount", String.valueOf(gatewayAmount));
        m.put("vnp_CurrCode","VND");
        m.put("vnp_TxnRef", txnRef);
        m.put("vnp_OrderInfo", orderInfo);
        m.put("vnp_OrderType","other");
        m.put("vnp_Locale","vn");
        m.put("vnp_ReturnUrl", VNPAYConfig.vnp_ReturnUrl); // will overwrite later with backend return incl paymentId
        m.put("vnp_IpAddr","127.0.0.1");
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        m.put("vnp_CreateDate", fmt.format(cld.getTime()));
        cld.add(Calendar.MINUTE,15);
        m.put("vnp_ExpireDate", fmt.format(cld.getTime()));
        return m;
    }

    private String buildSignedQuery(Map<String,String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (int i=0;i<keys.size();i++) {
            String k = keys.get(i); String v = params.get(k); if (v==null || v.isEmpty()) continue;
            hashData.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.US_ASCII));
            query.append(URLEncoder.encode(k, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(v, StandardCharsets.US_ASCII));
            if (i<keys.size()-1) { hashData.append('&'); query.append('&'); }
        }
        String secureHash = VNPAYConfig.hmacSHA512(VNPAYConfig.vnp_HashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);
        return query.toString();
    }

    private Payment persistPending(Long studentId, Long classRoomId, long amountVnd, String txnRef) {
        Payment p = new Payment();
        p.setOrderRef(UUID.randomUUID().toString().replace("-",""));
        p.setStudentId(studentId);
        p.setClassRoomId(classRoomId);
        p.setAmount(amountVnd);
        p.setCurrency("VND");
        p.setVnpTxnRef(txnRef);
        p.setStatus(PaymentStatus.PENDING);
        p.setPaid(Boolean.FALSE);
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return paymentRepository.save(p);
    }

    private Authentication auth() { return SecurityContextHolder.getContext().getAuthentication(); }

    // ---------------- create-url ----------------
    @GetMapping("/create-url")
    public ResponseEntity<PaymentUrlResponse> createPayment(@RequestParam Long classRoomId) {
        var classOpt = classEntityRepository.findById(classRoomId);
        if (classOpt.isEmpty()) return ResponseEntity.badRequest().body(new PaymentUrlResponse("error","ClassRoom not found"));
        var classRoom = classOpt.get();
        var course = classRoom.getCourse();
        if (course == null || course.getFee()==null) return ResponseEntity.status(500).body(new PaymentUrlResponse("error","Course fee not configured"));
        long amountVnd = course.getFee().longValue();

        Long studentId = null;
        Authentication a = auth();
        if (a!=null && a.isAuthenticated()) {
            try {
                studentId = Long.parseLong(a.getName());
                final Long sid = studentId; // make effectively final for lambda usage
                boolean isMember = classRoom.getStudents()!=null && classRoom.getStudents().stream().anyMatch(s->s.getId().equals(sid));
                if (!isMember) return ResponseEntity.status(403).body(new PaymentUrlResponse("error","Student not member of class"));
            } catch (Exception ignored) {}
        }

        String txnRef = randomTxnRef();
        Map<String,String> params = baseVnpParams(amountVnd, txnRef, "Thanh toan hoc phi lop:"+classRoomId+" ref:"+txnRef);
        Payment payment = persistPending(studentId, classRoomId, amountVnd, txnRef);
        params.put("vnp_ReturnUrl", VNPAYConfig.vnp_ReturnUrlBackend + "?paymentId=" + payment.getId());
        String paymentUrl = VNPAYConfig.vnp_Url + '?' + buildSignedQuery(params);
        return ResponseEntity.ok(new PaymentUrlResponse("success", payment.getId(), paymentUrl));
    }

    // ---------------- create-url-by-student ----------------
    @GetMapping("/create-url-by-student")
    public ResponseEntity<?> createUrlByStudent(@RequestParam Long studentId, @RequestParam(required = false) Long classRoomId) {
        if (studentId == null) return ResponseEntity.badRequest().body(Map.of("error","studentId required"));
        Authentication a = auth();
        if (a!=null && a.isAuthenticated()) {
            try { Long authId = Long.parseLong(a.getName()); if (!authId.equals(studentId)) return ResponseEntity.status(403).body(Map.of("error","Forbidden")); } catch (Exception ignored) {}
        }
        com.example.English.Center.Data.entity.classes.ClassRoom target;
        if (classRoomId != null) {
            var opt = classEntityRepository.findById(classRoomId); if (opt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","ClassRoom not found"));
            target = opt.get();
            boolean member = target.getStudents()!=null && target.getStudents().stream().anyMatch(s->s.getId().equals(studentId));
            if (!member) return ResponseEntity.status(403).body(Map.of("error","Student not member of class"));
        } else {
            var classes = classEntityRepository.findByStudents_Id(studentId);
            if (classes==null || classes.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Student not enrolled in any class"));
            if (classes.size()>1) return ResponseEntity.badRequest().body(Map.of("error","Multiple classes found, specify classRoomId","classRoomIds", classes.stream().map(c->c.getId()).toList()));
            target = classes.get(0);
        }
        var course = target.getCourse(); if (course==null || course.getFee()==null) return ResponseEntity.status(500).body(Map.of("error","Course fee not configured"));
        long amountVnd = course.getFee().longValue();
        // prevent duplicate
        var paidList = paymentRepository.findByStudentIdAndClassRoomIdAndStatus(studentId, target.getId(), PaymentStatus.SUCCESS);
        long paid = 0L; if (paidList!=null) for (var p: paidList) if (p.getAmount()!=null) paid += p.getAmount();
        if (paid >= amountVnd && amountVnd>0) return ResponseEntity.ok(Map.of("message","Already paid in full","classRoomId", target.getId(),"amount", amountVnd,"paidAmount", paid));
        String txnRef = randomTxnRef();
        Map<String,String> params = baseVnpParams(amountVnd, txnRef, "Thanh toan hoc phi student:"+studentId+" class:"+target.getId()+" ref:"+txnRef);
        Payment payment = persistPending(studentId, target.getId(), amountVnd, txnRef);
        params.put("vnp_ReturnUrl", VNPAYConfig.vnp_ReturnUrlBackend + "?paymentId=" + payment.getId());
        String paymentUrl = VNPAYConfig.vnp_Url + '?' + buildSignedQuery(params);
        return ResponseEntity.ok(Map.of("status","success","paymentId", payment.getId(),"url", paymentUrl));
    }

    // ---------------- Lookup ----------------
    @GetMapping("/{id}")
    public ResponseEntity<?> getPayment(@PathVariable Long id) {
        return paymentRepository.findById(id).<ResponseEntity<?>>map(p->ResponseEntity.ok(toMap(p))).orElseGet(()->ResponseEntity.notFound().build());
    }
    @GetMapping("/txn/{txnRef}")
    public ResponseEntity<?> getByTxn(@PathVariable String txnRef) {
        return paymentRepository.findByVnpTxnRef(txnRef).<ResponseEntity<?>>map(p->ResponseEntity.ok(toMap(p))).orElseGet(()->ResponseEntity.notFound().build());
    }
    private Map<String,Object> toMap(Payment p) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", p.getId()); m.put("orderRef", p.getOrderRef()); m.put("studentId", p.getStudentId()); m.put("classRoomId", p.getClassRoomId());
        m.put("amount", p.getAmount()); m.put("currency", p.getCurrency()); m.put("status", p.getStatus()!=null? p.getStatus().name(): null);
        m.put("vnpTxnRef", p.getVnpTxnRef()); m.put("vnpResponseCode", p.getVnpResponseCode()); m.put("createdAt", p.getCreatedAt()); m.put("updatedAt", p.getUpdatedAt());
        return m;
    }

    // ---------------- IPN (update DB) ----------------
    @GetMapping("/ipn") @Transactional
    public ResponseEntity<Map<String,String>> ipnGet(@RequestParam Map<String,String> params) { return ResponseEntity.ok(processIpnAndUpdate(params)); }
    @PostMapping(value="/ipn", consumes="application/x-www-form-urlencoded") @Transactional
    public ResponseEntity<Map<String,String>> ipnPost(@RequestParam Map<String,String> params) { return ResponseEntity.ok(processIpnAndUpdate(params)); }

    private Map<String,String> processIpnAndUpdate(Map<String,String> params) {
        try {
            log.info("IPN params: {}", params);
            // Build fields with ONLY vnp_* keys and URL-encoded values per VNPAY sample
            Map<String,String> fields = new HashMap<>();
            for (var e: params.entrySet()) {
                String k = e.getKey(); String v = e.getValue();
                if (k == null || v == null || v.isEmpty()) continue;
                if (!k.startsWith("vnp_")) continue; // exclude non-vnp params (e.g., paymentId)
                if ("vnp_SecureHash".equals(k) || "vnp_SecureHashType".equals(k)) continue; // exclude signature fields
                String encK = URLEncoder.encode(k, StandardCharsets.US_ASCII);
                String encV = URLEncoder.encode(v, StandardCharsets.US_ASCII);
                fields.put(encK, encV);
            }
            String secure = params.get("vnp_SecureHash");
            String sign = VNPAYConfig.hashAllFields(fields);
            if (secure==null || !sign.equalsIgnoreCase(secure)) return Map.of("RspCode","97","Message","Invalid Checksum");
            String txnRef = params.get("vnp_TxnRef"); if (txnRef==null) return Map.of("RspCode","01","Message","Order not Found");
            Optional<Payment> opt = paymentRepository.findByVnpTxnRef(txnRef); if (opt.isEmpty()) return Map.of("RspCode","01","Message","Order not Found");
            Payment payment = opt.get();
            String amountStr = params.get("vnp_Amount"); long gatewayAmt; try { gatewayAmt = Long.parseLong(amountStr); } catch(Exception ex){ return Map.of("RspCode","04","Message","Invalid Amount"); }
            long expected = payment.getAmount()*100L; if (gatewayAmt!=expected) return Map.of("RspCode","04","Message","Invalid Amount");
            if (payment.getStatus()!=PaymentStatus.PENDING) return Map.of("RspCode","02","Message","Order already confirmed");
            String rc = params.get("vnp_ResponseCode"); String ts = params.get("vnp_TransactionStatus");
            payment.setVnpResponseCode(rc); payment.setRawResponse(params.toString());
            if ("00".equals(rc) || "00".equals(ts)) { payment.setStatus(PaymentStatus.SUCCESS); payment.setPaid(Boolean.TRUE); }
            else { payment.setStatus(PaymentStatus.FAILED); payment.setPaid(Boolean.FALSE); }
            payment.setUpdatedAt(LocalDateTime.now()); paymentRepository.save(payment);
            return Map.of("RspCode","00","Message","Confirm Success");
        } catch (Exception ex) {
            log.error("IPN error", ex); return Map.of("RspCode","99","Message","Unknow error");
        }
    }

    // ---------------- Return (redirect + update if needed) ----------------
    @GetMapping("/return") @Transactional
    public ResponseEntity<?> returnHandler(@RequestParam Map<String,String> params) {
        Map<String,String> rsp = processIpnAndUpdate(params); // safe: will return 02 if already confirmed
        String rspCode = rsp.getOrDefault("RspCode","99");
        Long paymentId = null;
        String pid = params.get("paymentId"); if (pid!=null) try { paymentId = Long.parseLong(pid);} catch(Exception ignored){}
        if (paymentId==null) {
            String txnRef = params.get("vnp_TxnRef"); if (txnRef!=null) { var opt = paymentRepository.findByVnpTxnRef(txnRef); if (opt.isPresent()) paymentId = opt.get().getId(); }
        }
        String status;
        if ("00".equals(rspCode)) status = "success";
        else if ("02".equals(rspCode)) { // already confirmed; read actual status
            if (paymentId!=null) {
                var opt = paymentRepository.findById(paymentId);
                status = opt.map(p-> p.getStatus()==PaymentStatus.SUCCESS?"success": p.getStatus()==PaymentStatus.FAILED?"failed":"pending").orElse("success");
            } else status = "success";
        } else if ("97".equals(rspCode)) status = "invalid_signature";
        else if ("04".equals(rspCode)) status = "amount_mismatch";
        else if ("01".equals(rspCode)) status = "order_not_found";
        else if ("99".equals(rspCode)) status = "error";
        else status = "failed";
        String redirect = VNPAYConfig.vnp_ReturnUrlFrontend + "?status=" + status + (paymentId!=null?"&paymentId="+paymentId:"") + "&rspCode=" + rspCode;
        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND).header("Location", redirect).build();
    }

    // ---------------- Due for student ----------------
    @GetMapping("/due-for-student")
    public ResponseEntity<?> dueForStudent(@RequestParam Long studentId) {
        if (studentId==null) return ResponseEntity.badRequest().body(Map.of("error","studentId required"));
        var classes = classEntityRepository.findByStudents_Id(studentId); if (classes==null || classes.isEmpty()) return ResponseEntity.ok(List.of());
        List<Map<String,Object>> arr = new ArrayList<>();
        for (var cls: classes) {
            long fee = cls.getCourse()!=null && cls.getCourse().getFee()!=null? cls.getCourse().getFee().longValue():0L;
            var paidList = paymentRepository.findByStudentIdAndClassRoomIdAndStatus(studentId, cls.getId(), PaymentStatus.SUCCESS);
            long paid=0L; if (paidList!=null) for (var p: paidList) if (p.getAmount()!=null) paid+=p.getAmount();
            boolean full = paid>=fee && fee>0;
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("classRoomId", cls.getId()); m.put("className", cls.getName()); m.put("amount", fee); m.put("currency","VND");
            m.put("paidAmount", paid); m.put("paymentStatus", full?"PAID":"UNPAID"); m.put("isPaid", full);
            arr.add(m);
        }
        return ResponseEntity.ok(arr);
    }

    // ---------------- Overview ----------------
    @GetMapping("/student/{studentId}/overview")
    public ResponseEntity<?> overview(@PathVariable Long studentId) {
        if (studentId==null) return ResponseEntity.badRequest().body(Map.of("error","studentId required"));
        var classes = classEntityRepository.findByStudents_Id(studentId);
        List<Map<String,Object>> dues = new ArrayList<>();
        if (classes!=null) for (var cls: classes) { long fee = cls.getCourse()!=null && cls.getCourse().getFee()!=null? cls.getCourse().getFee().longValue():0L; dues.add(Map.of("classRoomId",cls.getId(),"className",cls.getName(),"amount",fee,"currency","VND")); }
        var payments = paymentRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        List<Map<String,Object>> history = new ArrayList<>(); if (payments!=null) for (var p: payments) history.add(toMap(p));
        return ResponseEntity.ok(Map.of("dues",dues,"payments",history));
    }

    // ---------------- Payment history (new endpoint) ----------------
    @GetMapping("/history")
    public ResponseEntity<?> paymentHistory(@RequestParam Long studentId,
                                            @RequestParam(required = false) Long classRoomId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to) {
        if (studentId == null) return ResponseEntity.badRequest().body(Map.of("error","studentId required"));
        // Base list
        var payments = paymentRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        if (payments == null || payments.isEmpty()) return ResponseEntity.ok(List.of());
        PaymentStatus filterStatus = null;
        if (status != null && !status.isBlank()) {
            try { filterStatus = PaymentStatus.valueOf(status.toUpperCase()); } catch (Exception ignored) {
                return ResponseEntity.badRequest().body(Map.of("error","Invalid status","allowed", Arrays.stream(PaymentStatus.values()).map(Enum::name).toList()));
            }
        }
        LocalDateTime fromDt = null; LocalDateTime toDt = null;
        try {
            if (from != null && !from.isBlank()) {
                fromDt = parseFlexibleDateTime(from);
            }
            if (to != null && !to.isBlank()) {
                toDt = parseFlexibleDateTime(to);
            }
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error","Invalid date format. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss"));
        }
        List<Map<String,Object>> result = new ArrayList<>();
        for (var p : payments) {
            if (classRoomId != null && !Objects.equals(p.getClassRoomId(), classRoomId)) continue;
            if (filterStatus != null && p.getStatus() != filterStatus) continue;
            if (fromDt != null && (p.getCreatedAt() == null || p.getCreatedAt().isBefore(fromDt))) continue;
            if (toDt != null && (p.getCreatedAt() == null || p.getCreatedAt().isAfter(toDt))) continue;
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("classRoomId", p.getClassRoomId());
            m.put("amount", p.getAmount());
            m.put("currency", p.getCurrency());
            m.put("status", p.getStatus()!=null? p.getStatus().name(): null);
            m.put("paid", p.getPaid());
            m.put("vnpTxnRef", p.getVnpTxnRef());
            m.put("vnpResponseCode", p.getVnpResponseCode());
            m.put("createdAt", p.getCreatedAt());
            m.put("updatedAt", p.getUpdatedAt());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // Helper to parse date or datetime flexibly
    private LocalDateTime parseFlexibleDateTime(String input) {
        if (input.length() == 10) { // yyyy-MM-dd
            return LocalDateTime.parse(input + "T00:00:00");
        }
        return LocalDateTime.parse(input); // expect ISO_LOCAL_DATE_TIME
    }

    // ---------------- Debug helpers (local testing) ----------------
    @GetMapping("/debug/force-success")
    @Transactional
    public ResponseEntity<?> forceSuccess(@RequestParam Long paymentId) {
        var opt = paymentRepository.findById(paymentId);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","payment not found"));
        Payment p = opt.get();
        p.setStatus(PaymentStatus.SUCCESS);
        p.setPaid(Boolean.TRUE);
        p.setVnpResponseCode("00");
        p.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(p);
        return ResponseEntity.ok(Map.of("message","forced to success","paymentId", p.getId()));
    }

    @PostMapping("/debug/simulate-ipn")
    @Transactional
    public ResponseEntity<?> simulateIpn(@RequestBody Map<String,String> body) {
        String txnRef = body.get("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","vnp_TxnRef required"));
        var opt = paymentRepository.findByVnpTxnRef(txnRef);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","payment not found by vnp_TxnRef"));
        Payment p = opt.get();
        String amount = body.getOrDefault("vnp_Amount", String.valueOf(p.getAmount() * 100L));
        Map<String,String> params = new HashMap<>();
        params.put("vnp_TmnCode", VNPAYConfig.vnp_TmnCode);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_Amount", amount);
        params.put("vnp_ResponseCode", body.getOrDefault("vnp_ResponseCode","00"));
        params.put("vnp_TransactionStatus", body.getOrDefault("vnp_TransactionStatus","00"));
        params.put("vnp_OrderInfo", body.getOrDefault("vnp_OrderInfo","simulate"));
        params.put("vnp_TransactionNo", body.getOrDefault("vnp_TransactionNo","999999"));
        // Sign like VNPAY: encode keys and values, then hash
        Map<String,String> toSign = new HashMap<>();
        for (var e: params.entrySet()) {
            String encK = URLEncoder.encode(e.getKey(), StandardCharsets.US_ASCII);
            String encV = URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII);
            toSign.put(encK, encV);
        }
        String secure = VNPAYConfig.hashAllFields(toSign);
        params.put("vnp_SecureHash", secure);
        Map<String,String> rsp = processIpnAndUpdate(params);
        var refreshed = paymentRepository.findById(p.getId());
        return ResponseEntity.ok(Map.of(
                "RspCode", rsp.get("RspCode"),
                "Message", rsp.get("Message"),
                "paymentId", p.getId(),
                "status", refreshed.map(Payment::getStatus).map(Enum::name).orElse("UNKNOWN")
        ));
    }
}
