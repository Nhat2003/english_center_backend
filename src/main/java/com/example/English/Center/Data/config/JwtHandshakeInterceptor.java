package com.example.English.Center.Data.config;

import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.repository.users.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletReq) {
            HttpServletRequest httpReq = servletReq.getServletRequest();
            // check Authorization header or token query param
            String token = null;
            String auth = httpReq.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) token = auth.substring(7);
            if (token == null) token = httpReq.getParameter("token");

            if (token != null) {
                try {
                    String username = jwtUtil.extractUsername(token);
                    if (username != null) {
                        Optional<User> uo = userRepository.findByUsername(username);
                        if (uo.isPresent()) {
                            User u = uo.get();
                            attributes.put("userId", u.getId());
                            // set a Principal for STOMP user destinations
                            attributes.put("principal", new StompPrincipal(u.getId().toString()));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {

    }

    public static class StompPrincipal implements Principal {
        private final String name;
        public StompPrincipal(String name) { this.name = name; }
        @Override public String getName() { return name; }
    }
}
