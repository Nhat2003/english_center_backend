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

                // 🔒 Class & student management
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/class-rooms/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/class-rooms/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/class-students/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                // 🔒 User management
                .requestMatchers("/users/**", "/teachers/**", "/students/**", "/classes/**", "/courses/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/teachers/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/students/**").hasAnyRole("STUDENT", "ADMIN")

                // 🔒 Schedule
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/schedule/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                // ✅ Assignment endpoints
                // - TEACHER có thể tạo, cập nhật, xóa bài tập
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/assignments/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/assignments/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/assignments/**").hasAnyRole("TEACHER", "ADMIN")
                // - STUDENT, TEACHER, ADMIN có thể xem bài tập
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/assignments/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/assignments/classroom/**").hasAnyRole("TEACHER", "ADMIN")

                // ✅ Submission endpoints
                // - STUDENT and ADMIN can submit
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/submissions/**").hasAnyRole("STUDENT", "ADMIN")
                // - TEACHER chấm điểm, xem danh sách
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

                // 🔑 ADMIN: allow ADMIN to access any API (kept after specific matchers so other role rules & public endpoints still work)
                .requestMatchers("/**").hasRole("ADMIN")

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