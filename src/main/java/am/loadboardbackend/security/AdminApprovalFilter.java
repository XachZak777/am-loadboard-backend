package am.loadboardbackend.security;

import am.loadboardbackend.model.User;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminApprovalFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.startsWith("/api/auth") || path.startsWith("/api/validate")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.equals("/api/carriers/profile") || path.equals("/api/brokers/profile")
                || path.startsWith("/api/carriers/documents/")
                || path.startsWith("/api/brokers/documents/")
                || path.startsWith("/api/files/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {

            if (user.getRole() != null && user.getRole().name().equals("ROLE_ADMIN")) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!user.isEmailVerified()) {
                writeError(response, 403, "Please verify your email address");
                return;
            }

            if (!user.isAdminApproved()) {
                writeError(response, 403, "Account pending admin approval");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("message", message, "status", status));
    }
}
