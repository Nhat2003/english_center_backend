    package com.example.English.Center.Data.config;

import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.Map;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Object p = attributes.get("principal");
        if (p instanceof Principal) return (Principal) p;
        if (p instanceof String) return new SimplePrincipal((String) p);
        return super.determineUser(request, wsHandler, attributes);
    }

    static class SimplePrincipal implements Principal {
        private final String name;
        SimplePrincipal(String name) { this.name = name; }
        @Override public String getName() { return name; }
    }
}

