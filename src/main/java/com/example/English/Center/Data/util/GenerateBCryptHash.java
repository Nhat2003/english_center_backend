package com.example.English.Center.Data.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hash
 * Run: mvn exec:java -Dexec.mainClass="com.example.English.Center.Data.util.GenerateBCryptHash"
 */
public class GenerateBCryptHash {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        String[] passwords = {"Admin@123", "password", "admin123", "Student@123"};

        System.out.println("================================================================================");
        System.out.println("BCrypt Password Hash Generator");
        System.out.println("================================================================================");

        for (String plainPassword : passwords) {
            String hashedPassword = encoder.encode(plainPassword);

            System.out.println("\n[Password: " + plainPassword + "]");
            System.out.println("Hash:   " + hashedPassword);
            System.out.println("Length: " + hashedPassword.length());

            // Verify
            boolean matches = encoder.matches(plainPassword, hashedPassword);
            System.out.println("Valid:  " + (matches ? "YES" : "NO"));

            System.out.println("\nSQL INSERT:");
            System.out.println("DELETE FROM users WHERE username = 'admin';");
            System.out.println("INSERT INTO users (username, password, role, is_active, full_name, created_at, updated_at)");
            System.out.println("VALUES ('admin', '" + hashedPassword + "', 'ADMIN', true, 'Administrator', now(), now());");
            System.out.println("--------------------------------------------------------------------------------");
        }

        System.out.println("\n================================================================================");
        System.out.println("USAGE:");
        System.out.println("1. Copy one of the SQL INSERT statements above");
        System.out.println("2. Run it in pgAdmin or psql");
        System.out.println("3. Test login with username: admin, password: [the password you chose]");
        System.out.println("================================================================================");
    }
}

