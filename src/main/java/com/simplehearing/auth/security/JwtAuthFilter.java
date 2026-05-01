package com.simplehearing.auth.security;

import com.simplehearing.common.tenant.TenantContext;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public JwtAuthFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = tokenService.validateAndExtract(token);

            UUID userId = UUID.fromString(claims.getSubject());
            UUID orgId  = UUID.fromString(claims.get("orgId", String.class));
            String clinicIdStr = claims.get("clinicId", String.class);
            UUID clinicId = clinicIdStr != null ? UUID.fromString(clinicIdStr) : null;
            String role   = claims.get("role", String.class);

            User user = userRepository.findById(userId)
                    .filter(User::isActive)
                    .orElse(null);

            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserPrincipal principal = new UserPrincipal(user);

                // Build authorities from ALL roles in the token (primary + additional)
                String rolesStr = claims.get("roles", String.class);
                List<GrantedAuthority> authorities = (rolesStr != null && !rolesStr.isBlank())
                        ? Arrays.stream(rolesStr.split(","))
                                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.trim()))
                                .toList()
                        : principal.getAuthorities().stream().map(a -> (GrantedAuthority) a).toList();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                TenantContext.set(orgId, clinicId, userId, role);
            }

        } catch (JwtException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            log.warn("Could not authenticate request", ex);
            SecurityContextHolder.clearContext();
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
