package com.staffone.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = h.substring(7);
        if (!jwt.valid(token)) {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Invalid token\"}");
            return;
        }

        Claims c = jwt.parse(token);
        UUID tenantId = UUID.fromString(c.get("tenant_id", String.class));
        TenantContext.set(tenantId);

        StaffOnePrincipal p = new StaffOnePrincipal(
            UUID.fromString(c.getSubject()), tenantId,
            c.get("email",       String.class),
            c.get("role",        String.class),
            c.get("tenant_mode", String.class));

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + p.role()))));

        try {
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}
