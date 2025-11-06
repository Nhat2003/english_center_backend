package com.example.English.Center.Data.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.English.Center.Data.config.JwtAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public WebSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests()
                // 🔓 Public endpoints
                .requestMatchers("/users/login", "/users/register", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Announcements & Notifications (specific rules before general /classes/** admin rule)
                    // Teachers/Admin can create announcements for a class
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/classes/*/announcements").hasAnyRole("TEACHER", "ADMIN")
                // Students/Teachers can read announcements
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/classes/*/announcements").hasAnyRole("STUDENT", "TEACHER")
                // Students (owner) and ADMIN can access student notifications endpoints (controller still checks owner)
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/classes/students/*/notifications").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/classes/notifications/*/read").hasAnyRole("STUDENT", "ADMIN")

                // 🔒 Class & student management
                // Allow GET on both exact path and subpaths for class-rooms
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/class-rooms", "/class-rooms/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                // Admin-only for create/update/delete class-rooms (restrict to write methods)
                // Allow ADMIN and TEACHER to create/update class-rooms (so teachers can create classes)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/class-rooms/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/class-rooms/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/class-rooms/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/class-students/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                // Allow teachers to fetch classes they manage
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/class-rooms/by-teacher/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/class-rooms/by-student/**").hasAnyRole("STUDENT", "ADMIN")

                // Allow GET /teachers and GET /students for appropriate roles
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/teachers/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/students/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                // 🔒 User management (write operations remain admin-only)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/users/**", "/teachers/**", "/students/**", "/classes/**", "/courses/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/users/**", "/teachers/**", "/students/**", "/classes/**", "/courses/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/users/**", "/teachers/**", "/students/**", "/classes/**", "/courses/**").hasRole("ADMIN")

                // 🔒 Schedule
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/schedule/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                // Allow websocket handshake and SockJS endpoint
                .requestMatchers("/ws/**", "/ws/chat/**", "/topic/**", "/app/**", "/user/**").permitAll()

                // ✅ Assignment endpoints
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/assignments/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/assignments/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/assignments/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/assignments/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/assignments/classroom/**").hasAnyRole("TEACHER", "ADMIN")

                // ✅ Submission endpoints
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/submissions/**").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/submissions/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/submissions/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                // ✅ Attendance endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/attendance/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/attendance/**").hasAnyRole("TEACHER", "ADMIN")

                // ✅ Allow file upload (for assignment attachments)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/files/upload").hasAnyRole("TEACHER", "ADMIN")

                // 🔒 Class Document Management
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/class-documents/upload/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/class-documents/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/class-documents/class/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/class-documents/download/**").permitAll() // Public access to document download


                // Any other request needs authentication
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin().disable()
                .httpBasic().disable();

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}