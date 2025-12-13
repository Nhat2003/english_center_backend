package com.example.English.Center.Data.controller.payments;

import com.example.English.Center.Data.config.VNPAYConfig;
import com.example.English.Center.Data.entity.payments.Payment;
import com.example.English.Center.Data.entity.payments.PaymentStatus;
import com.example.English.Center.Data.repository.classes.ClassEntityRepository;
import com.example.English.Center.Data.repository.payments.PaymentRepository;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.service.payments.VnpayResponseCodes;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final ClassEntityRepository classEntityRepository;
    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;

    public PaymentController(ClassEntityRepository classEntityRepository, PaymentRepository paymentRepository, StudentRepository studentRepository) {
        this.classEntityRepository = classEntityRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
    }

    // ---------------- DTO ----------------
    public static class PaymentUrlResponse {
        private String status;
        private String url;
        private Long paymentId;
        private String paymentMethod = "VNPAY"; // default provider
        public PaymentUrlResponse() {}
        public PaymentUrlResponse(String status, Long paymentId, String url) { this.status = status; this.paymentId = paymentId; this.url = url; }
        public PaymentUrlResponse(String status, String url) { this.status = status; this.url = url; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Long getPaymentId() { return paymentId; }
        public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
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

    // compute the due date: Sunday of the first week that includes classStartDate
    private LocalDate computeDueDate(LocalDate classStart) {
        if (classStart == null) return null;
        // find the sunday of the same week (week ending sunday). If classStart is already Sunday, keep it.
        DayOfWeek dow = classStart.getDayOfWeek();
        int daysToSunday = DayOfWeek.SUNDAY.getValue() - dow.getValue();
        if (daysToSunday < 0) daysToSunday += 7; // move forward to next Sunday
        return classStart.plusDays(daysToSunday);
    }

    private Payment persistPending(Long studentId, Long classRoomId, long amountVnd, String txnRef, LocalDate dueDate) {
        Payment p = new Payment();
        p.setOrderRef(UUID.randomUUID().toString().replace("-",""));
        p.setStudentId(studentId);
        p.setClassRoomId(classRoomId);
        p.setAmount(amountVnd);
        p.setCurrency("VND");
        p.setVnpTxnRef(txnRef);
        p.setStatus(PaymentStatus.PENDING);
        p.setPaid(Boolean.FALSE);
        p.setDueDate(dueDate);
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
        // compute due date from class start date
        LocalDate dueDate = computeDueDate(classRoom.getStartDate());
        Payment payment = persistPending(studentId, classRoomId, amountVnd, txnRef, dueDate);
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
        // compute due date
        LocalDate dueDate = computeDueDate(target.getStartDate());
        Payment payment = persistPending(studentId, target.getId(), amountVnd, txnRef, dueDate);
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
        m.put("paymentMethod","VNPAY");
        m.put("dueDate", p.getDueDate());
        // Add a human-readable message for the vnp response code so frontend can display a friendly message
        String vnpMsg = VnpayResponseCodes.getMessage(p.getVnpResponseCode());
        if (vnpMsg != null) m.put("vnpResponseMessage", vnpMsg);
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
            if ("00".equals(rc) || "00".equals(ts)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaid(Boolean.TRUE);
            } else {
                // keep as PENDING for incomplete/canceled/failed at gateway stage
                payment.setPaid(Boolean.FALSE);
                // Optionally keep status unchanged (PENDING). Do not set FAILED/CANCELED here to follow requirement.
            }
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            // Build response with human-readable message for frontend
            String rspMsg = VnpayResponseCodes.getMessage(rc);
            if (rspMsg == null) rspMsg = "Confirm Success";
            return Map.of("RspCode","00","Message","Confirm Success","vnpResponseMessage", rspMsg);
        } catch (Exception ex) {
            log.error("IPN error", ex); return Map.of("RspCode","99","Message","Unknow error");
        }
    }

    // ---------------- Return (redirect + update if needed) ----------------
    @GetMapping("/return") @Transactional
    public ResponseEntity<?> returnHandler(@RequestParam Map<String,String> params) {
        Map<String,String> rsp = processIpnAndUpdate(params);
        Long paymentId = null;
        String pid = params.get("paymentId"); if (pid!=null) try { paymentId = Long.parseLong(pid);} catch(Exception ignored){}
        Payment payment = null;
        if (paymentId==null) {
            String txnRef = params.get("vnp_TxnRef");
            if (txnRef!=null) { var opt = paymentRepository.findByVnpTxnRef(txnRef); if (opt.isPresent()) { payment = opt.get(); paymentId = payment.getId(); } }
        } else {
            var opt = paymentRepository.findById(paymentId); if (opt.isPresent()) payment = opt.get();
        }
        String status;
        if (payment != null) {
            if (payment.getStatus() == PaymentStatus.SUCCESS) status = "success";
            else if (payment.getStatus() == PaymentStatus.FAILED) status = "failed";
            else if (payment.getStatus() == PaymentStatus.CANCELED) status = "canceled";
            else status = "pending"; // default
        } else {
            status = "pending";
        }
        String redirect = VNPAYConfig.vnp_ReturnUrlFrontend + "?status=" + status + (paymentId!=null?"&paymentId="+paymentId:"") + "&rspCode=" + rsp.getOrDefault("RspCode","99");
        // Append human friendly message to redirect (URL-encoded) so frontend can show it immediately
        String rspMsg = rsp.getOrDefault("vnpResponseMessage", "");
        if (rspMsg != null && !rspMsg.isBlank()) {
            redirect += "&rspMsg=" + URLEncoder.encode(rspMsg, StandardCharsets.UTF_8);
        }
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
            m.put("dueDate", computeDueDate(cls.getStartDate()));
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
        if (classes!=null) for (var cls: classes) { long fee = cls.getCourse()!=null && cls.getCourse().getFee()!=null? cls.getCourse().getFee().longValue():0L; dues.add(Map.of("classRoomId",cls.getId(),"className",cls.getName(),"amount",fee,"currency","VND","dueDate", computeDueDate(cls.getStartDate()))); }
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
            // add friendly message for known vnp codes
            m.put("vnpResponseMessage", VnpayResponseCodes.getMessage(p.getVnpResponseCode()));
            m.put("createdAt", p.getCreatedAt());
            m.put("updatedAt", p.getUpdatedAt());
            m.put("paymentMethod","VNPAY");
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

    // ---------------- Admin: payments by class ----------------
    @GetMapping("/admin/classes/{classRoomId}/payments")
    public ResponseEntity<?> adminListByClass(@PathVariable Long classRoomId,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String from,
                                              @RequestParam(required = false) String to) {
        if (classRoomId == null) return ResponseEntity.badRequest().body(Map.of("error","classRoomId required"));
        var classOpt = classEntityRepository.findById(classRoomId);
        if (classOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","ClassRoom not found"));
        PaymentStatus filterStatus = null;
        if (status != null && !status.isBlank()) {
            try { filterStatus = PaymentStatus.valueOf(status.toUpperCase()); } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error","Invalid status","allowed", Arrays.stream(PaymentStatus.values()).map(Enum::name).toList()));
            }
        }
        LocalDateTime fromDt = null, toDt = null;
        try {
            if (from != null && !from.isBlank()) fromDt = parseFlexibleDateTime(from);
            if (to != null && !to.isBlank()) toDt = parseFlexibleDateTime(to);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error","Invalid date format. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss"));
        }
        List<Payment> list = (filterStatus == null)
                ? paymentRepository.findByClassRoomIdOrderByCreatedAtDesc(classRoomId)
                : paymentRepository.findByClassRoomIdAndStatusOrderByCreatedAtDesc(classRoomId, filterStatus);
        List<Map<String,Object>> result = new ArrayList<>();
        Map<Long,String> nameCache = new HashMap<>();
        for (var p: list) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("studentId", p.getStudentId());
            m.put("classRoomId", p.getClassRoomId());
            m.put("amount", p.getAmount());
            m.put("currency", p.getCurrency());
            m.put("status", p.getStatus()!=null? p.getStatus().name(): null);
            m.put("paid", p.getPaid());
            m.put("vnpTxnRef", p.getVnpTxnRef());
            m.put("vnpResponseCode", p.getVnpResponseCode());
            // add friendly message for known vnp codes
            m.put("vnpResponseMessage", VnpayResponseCodes.getMessage(p.getVnpResponseCode()));
            m.put("createdAt", p.getCreatedAt());
            m.put("updatedAt", p.getUpdatedAt());
            m.put("paymentMethod","VNPAY");
            Long sid = p.getStudentId();
            if (sid != null) {
                String nm = nameCache.get(sid);
                if (nm == null) {
                    nm = studentRepository.findById(sid).map(Student::getFullName).orElse(null);
                    if (nm != null) nameCache.put(sid, nm);
                }
                m.put("studentName", nm);
            }
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/admin/classes/{classRoomId}/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<?> adminSummaryByClass(@PathVariable Long classRoomId) {
        if (classRoomId == null) return ResponseEntity.badRequest().body(Map.of("error","classRoomId required"));
        var classOpt = classEntityRepository.findById(classRoomId);
        if (classOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","ClassRoom not found"));
        var list = paymentRepository.findByClassRoomIdOrderByCreatedAtDesc(classRoomId);
        long total = 0L, totalSuccess = 0L, totalFailed = 0L, totalPending = 0L;
        int count = 0, countSuccess = 0, countFailed = 0, countPending = 0;
        for (var p: list) {
            long amt = p.getAmount() != null ? p.getAmount() : 0L;
            total += amt; count++;
            if (p.getStatus() == PaymentStatus.SUCCESS) { totalSuccess += amt; countSuccess++; }
            else if (p.getStatus() == PaymentStatus.FAILED) { totalFailed += amt; countFailed++; }
            else { totalPending += amt; countPending++; }
        }
        // Build per-student breakdown
        var classRoom = classOpt.get();
        var course = classRoom.getCourse();
        long fee = (course != null && course.getFee()!=null) ? course.getFee().longValue() : 0L;
        List<Map<String,Object>> studentsArr = new ArrayList<>();
        if (classRoom.getStudents() != null) {
            for (Student s : classRoom.getStudents()) {
                long paidAmount = 0L;
                var successPays = paymentRepository.findByStudentIdAndClassRoomIdAndStatus(s.getId(), classRoomId, PaymentStatus.SUCCESS);
                LocalDateTime latestPaidAt = null;
                if (successPays != null) {
                    for (var sp : successPays) {
                        if (sp.getAmount()!=null) paidAmount += sp.getAmount();
                        LocalDateTime cand = sp.getUpdatedAt()!=null ? sp.getUpdatedAt() : sp.getCreatedAt();
                        if (cand != null && (latestPaidAt == null || cand.isAfter(latestPaidAt))) latestPaidAt = cand;
                    }
                }
                boolean isPaid = fee > 0 && paidAmount >= fee;
                // latest status (if any)
                String latestStatus = null;
                var allPays = paymentRepository.findByStudentIdAndClassRoomId(s.getId(), classRoomId);
                if (allPays != null && !allPays.isEmpty()) {
                    allPays.sort((a,b)-> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    latestStatus = allPays.get(0).getStatus()!=null ? allPays.get(0).getStatus().name() : null;
                }
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("studentId", s.getId());
                row.put("studentName", s.getFullName());
                row.put("paidAmount", paidAmount);
                row.put("requiredAmount", fee);
                row.put("paymentStatus", isPaid ? "PAID" : "UNPAID");
                row.put("latestPaymentStatus", latestStatus);
                if (latestPaidAt != null) row.put("paidAt", latestPaidAt);
                studentsArr.add(row);
            }
        }
        Map<String,Object> resp = new LinkedHashMap<>();
        resp.put("classRoomId", classRoomId);
        resp.put("count", count);
        resp.put("countSuccess", countSuccess);
        resp.put("countFailed", countFailed);
        resp.put("countPending", countPending);
        resp.put("total", total);
        resp.put("totalSuccess", totalSuccess);
        resp.put("totalFailed", totalFailed);
        resp.put("totalPending", totalPending);
        resp.put("currency", "VND");
        resp.put("paymentMethod", "VNPAY");
        resp.put("students", studentsArr);
        return ResponseEntity.ok(resp);
    }

    // User cancels payment manually (e.g., closes VNPAY or presses cancel)
    @PostMapping("/cancel")
    @Transactional
    public ResponseEntity<?> cancelPayment(@RequestBody Map<String,String> body) {
        String idStr = body.get("paymentId");
        String txnRef = body.get("vnp_TxnRef");
        Optional<Payment> opt = Optional.empty();
        if (idStr != null) {
            try { opt = paymentRepository.findById(Long.parseLong(idStr)); } catch (Exception ignored) {}
        }
        if (opt.isEmpty() && txnRef != null) opt = paymentRepository.findByVnpTxnRef(txnRef);
        if (opt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","payment not found"));
        Payment p = opt.get();
        if (p.getStatus() == PaymentStatus.PENDING) {
            p.setStatus(PaymentStatus.CANCELED);
            p.setPaid(Boolean.FALSE);
            p.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(p);
            return ResponseEntity.ok(Map.of("message","canceled","paymentId", p.getId(), "status", p.getStatus().name()));
        }
        return ResponseEntity.ok(Map.of("message","no change","paymentId", p.getId(), "status", p.getStatus().name()));
    }
}
//Don SUCCESS
