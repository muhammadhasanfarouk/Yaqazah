package com.yaqazah.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@NullMarked
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Skip CORS preflight requests
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);

            try {
                email = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("Could not extract username from token", e);
            }
        }

        if (email != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            List<String> roles = jwtUtil.extractRoles(jwt);

            if (roles == null) {
                roles = java.util.Collections.emptyList();
            }

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UserDetails userDetails =
                    new org.springframework.security.core.userdetails.User(
                            email,
                            "",
                            authorities
                    );

            if (jwtUtil.validateToken(jwt, userDetails)) {

                String client = jwtUtil.extractClient(jwt);
                String requestURI = request.getRequestURI();

                if (requestURI.startsWith("/api/web/")
                        && !"web".equals(client)) {

                    response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Web token required."
                    );
                    return;
                }

                if (requestURI.startsWith("/api/mobile/")
                        && !"mobile".equals(client)) {

                    response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Mobile token required."
                    );
                    return;
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                authorities
                        );

                auth.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}