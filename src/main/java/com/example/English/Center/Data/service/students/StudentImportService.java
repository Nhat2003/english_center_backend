package com.example.English.Center.Data.service.students;

import com.example.English.Center.Data.dto.students.StudentImportRowResult;
import com.example.English.Center.Data.dto.students.StudentImportSummary;
import com.example.English.Center.Data.entity.students.Student;
import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.entity.users.UserRole;
import com.example.English.Center.Data.repository.students.StudentRepository;
import com.example.English.Center.Data.repository.users.UserRepository;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Date;
import java.util.*;

@Service
public class StudentImportService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentImportService(UserRepository userRepository, StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public StudentImportSummary importFromExcel(MultipartFile file, Long assignClassId) throws Exception {
        // Basic validations
        String orig = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = FilenameUtils.getExtension(orig).toLowerCase();
        if (!List.of("xlsx","xls").contains(ext)) throw new IllegalArgumentException("File must be xls or xlsx");

        List<StudentImportRowResult> results = new ArrayList<>();
        int total = 0, processed = 0, successes = 0, skipped = 0, failed = 0;

        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIt = sheet.iterator();
            if (!rowIt.hasNext()) throw new IllegalArgumentException("Empty sheet");

            Row header = rowIt.next();
            Map<String,Integer> colIndex = new HashMap<>();
            for (Cell c : header) {
                String v = c.getStringCellValue().trim();
                colIndex.put(v.toLowerCase(), c.getColumnIndex());
            }

            // required column: fullname
            if (!colIndex.containsKey("fullname") && !colIndex.containsKey("họ và tên") && !colIndex.containsKey("họ tên")) {
                throw new IllegalArgumentException("Header must contain 'fullname' (or 'Họ và tên') column");
            }

            DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            int rowNum = 1; // header at row 0
            DataFormatter formatter = new DataFormatter();
            while (rowIt.hasNext()) {
                Row r = rowIt.next();
                rowNum++;
                total++;
                try {
                    String fullname = getCellString(r, colIndex, List.of("fullname","họ và tên","họ tên"), formatter);
                    if (fullname == null || fullname.isBlank()) {
                        results.add(StudentImportRowResult.builder().rowNumber(rowNum).status("FAILED").message("fullname missing").build());
                        failed++;
                        continue;
                    }
                    // build username: take last token (given name) e.g. Nguyễn Văn Long -> Long
                    String namePart = buildUsernameFromFullname(fullname); // normalized, ASCII-only, e.g. "Sang"
                    // Format name part with capitalized first letter and rest lowercase (Sang -> Sang)
                    if (!namePart.isBlank()) {
                        if (namePart.length() == 1) namePart = namePart.toUpperCase();
                        else namePart = namePart.substring(0,1).toUpperCase() + namePart.substring(1).toLowerCase();
                    } else {
                        namePart = "";
                    }
                    // final base username like "studentSang"
                    String baseUsername = "student" + namePart;
                    String finalUsername = baseUsername;
                    int suffix = 1;
                    while (userRepository.existsByUsername(finalUsername)) {
                        finalUsername = baseUsername + suffix;
                        suffix++;
                    }

                    String email = getCellString(r, colIndex, List.of("email"), formatter);
                    // validate email format if present
                    if (email != null && !email.isBlank()) {
                        String e = email.trim();
                        boolean emailValid = e.contains("@") && e.contains(".");
                        if (!emailValid) {
                            results.add(StudentImportRowResult.builder().rowNumber(rowNum).status("FAILED").message("Invalid email format: " + e).build());
                            failed++;
                            continue;
                        }
                        // check uniqueness (case-insensitive)
                        if (studentRepository.findByEmailIgnoreCase(e).isPresent()) {
                            results.add(StudentImportRowResult.builder().rowNumber(rowNum).status("SKIPPED").message("Email already exists: " + e).build());
                            skipped++;
                            continue;
                        }
                    }

                    String phone = getCellString(r, colIndex, List.of("phone","số điện thoại"), formatter);
                    String address = getCellString(r, colIndex, List.of("address","địa chỉ"), formatter);

                    // Parse DOB and joinedAt using helper
                    DateTimeFormatter[] formats = new DateTimeFormatter[] { iso, DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("d/M/yyyy") };
                    LocalDate dob = parseDateFromRow(r, colIndex, List.of("dob","ngày sinh","ngay sinh"), formatter, formats);
                    LocalDate joinedAt = parseDateFromRow(r, colIndex, List.of("joinedat","joined_at","ngày vào trung tâm","ngay vao trung tam"), formatter, formats);

                    // Gender: normalize common values to MALE / FEMALE; fallback to raw trimmed value
                    String genderRaw = getCellString(r, colIndex, List.of("gender","giới tính","gioi tinh"), formatter);
                    String gender = normalizeGender(genderRaw);

                    // create User
                    User u = new User();
                    u.setUsername(finalUsername);
                    u.setFullName(fullname);
                    u.setRole(UserRole.STUDENT);
                    u.setIsActive(true);
                    u.setPassword(passwordEncoder.encode("123456"));
                    User savedUser = userRepository.save(u);

                    // create Student
                    Student s = new Student();
                    s.setUser(savedUser);
                    s.setFullName(fullname);
                    s.setEmail(email);
                    s.setPhone(phone);
                    s.setAddress(address);
                    s.setDob(dob);
                    s.setGender(gender);
                    s.setJoinedAt(joinedAt);
                    // if assignClassId provided, store as className temporarily (front-end/backend can map properly later)
                    if (assignClassId != null) {
                        s.setClassName(String.valueOf(assignClassId));
                    }
                    Student savedStudent = studentRepository.save(s);

                    results.add(StudentImportRowResult.builder()
                            .rowNumber(rowNum)
                            .status("SUCCESS")
                            .userId(savedUser.getId())
                            .studentId(savedStudent.getId())
                            .message("Imported")
                            .build());
                    successes++;
                    processed++;
                } catch (Exception rowEx) {
                    results.add(StudentImportRowResult.builder().rowNumber(rowNum).status("FAILED").message(rowEx.getMessage()).build());
                    failed++;
                }
            }
        }

        return StudentImportSummary.builder()
                .totalRows(total)
                .processed(processed)
                .successes(successes)
                .skipped(skipped)
                .failed(failed)
                .results(results)
                .build();
    }

    private String getCellString(Row r, Map<String,Integer> colIndex, List<String> keys, DataFormatter formatter) {
        for (String k : keys) {
            Integer idx = colIndex.get(k.toLowerCase());
            if (idx != null) {
                Cell c = r.getCell(idx);
                if (c == null) return null;
                String v = formatter.formatCellValue(c);
                return v == null ? null : v.trim();
            }
        }
        return null;
    }

    private String buildUsernameFromFullname(String fullname) {
        // take last token as given name
        String[] parts = fullname.trim().split("\\s+");
        String name = parts.length == 0 ? fullname.trim() : parts[parts.length - 1];
        // Normalize to remove diacritics (e.g. 'Sáng' -> 'Sang'), handle 'đ' explicitly
        String n = Normalizer.normalize(name, Normalizer.Form.NFD);
        // remove combining diacritical marks
        n = n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // replace special Vietnamese character đ/Đ
        n = n.replace('đ', 'd').replace('Đ', 'D');
        // remove any non-alphanumeric characters and spaces
        n = n.replaceAll("[^A-Za-z0-9]", "");
        return n;
    }

    private LocalDate parseDateFromRow(Row r, Map<String,Integer> colIndex, List<String> keys, DataFormatter formatter, DateTimeFormatter[] formats) {
        for (String k : keys) {
            Integer idx = colIndex.get(k.toLowerCase());
            if (idx != null) {
                Cell c = r.getCell(idx);
                if (c != null) {
                    if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
                        Date d = c.getDateCellValue();
                        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    } else {
                        String v = formatter.formatCellValue(c).trim();
                        if (!v.isBlank()) {
                            // try parsing with multiple formats
                            for (DateTimeFormatter fmt : formats) {
                                try {
                                    return LocalDate.parse(v, fmt);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String normalizeGender(String genderRaw) {
        if (genderRaw != null && !genderRaw.isBlank()) {
            String gNorm = Normalizer.normalize(genderRaw.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
            if (gNorm.equals("nam") || gNorm.equals("male") || gNorm.equals("m")) {
                return "MALE";
            } else if (gNorm.equals("nu") || gNorm.equals("female") || gNorm.equals("f")) {
                return "FEMALE";
            } else {
                if (gNorm.contains("nam")) return "MALE";
                else if (gNorm.contains("nu")) return "FEMALE";
                else return genderRaw.trim();
            }
        }
        return null;
    }
}
