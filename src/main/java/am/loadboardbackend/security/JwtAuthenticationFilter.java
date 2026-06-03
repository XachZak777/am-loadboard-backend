package am.loadboardbackend.security;

import am.loadboardbackend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final Set<String> PUBLIC_PATTERNS = Set.of(
            "/api/auth/**",
            "/api/validate/**",
            "/api/carriers/register",
            "/api/brokers/register",
            "/api/dealers/register",
            "/api/loads/**",
            "/api/files/**"
    );

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if ("GET".equalsIgnoreCase(request.getMethod())
                && PATH_MATCHER.match("/api/ratings/**", path)) {
            return true;
        }
        return PUBLIC_PATTERNS.stream().anyMatch(p -> PATH_MATCHER.match(p, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String cookieToken = extractFromCookie(request);
        if (cookieToken != null && jwtUtil.validate(cookieToken)) {
            setAuthentication(cookieToken);
            filterChain.doFilter(request, response);
            return;
        }

        // Cookie absent or invalid — check Authorization: Bearer header.
        // This covers the post-registration profile-save flow where the server
        // issues a token in the response body (no cookie yet) but a stale cookie
        // from a previous session may still be present in the browser.
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String headerToken = header.substring(7);
            if (jwtUtil.validate(headerToken)) {
                setAuthentication(headerToken);
            } else if (!isPublicPath(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Invalid or expired token\"}");
                return;
            }
        } else if (cookieToken != null && !isPublicPath(request)) {
            // Had a cookie but it was invalid, and no Bearer token provided
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Session expired\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(String token) {
        UUID userId = jwtUtil.extractUserId(token);
        userRepository.findById(userId).ifPresent(user -> {
            var authorities = user.getRole() == null
                    ? List.<SimpleGrantedAuthority>of()
                    : List.of(new SimpleGrantedAuthority(user.getRole().name()));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        });
    }

    private String extractFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("jwt".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
