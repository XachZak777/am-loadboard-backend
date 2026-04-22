package am.loadboardbackend.security;

import am.loadboardbackend.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminApprovalFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Allow auth + validation flows, and anything public.
        if (path.startsWith("/api/auth") || path.startsWith("/api/validate")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow profile completion and document upload regardless of approval status,
        // so users can submit their info and documents before the admin approves them.
        if (path.equals("/api/carriers/profile") || path.equals("/api/brokers/profile")
                || path.startsWith("/api/carriers/documents/")
                || path.startsWith("/api/brokers/documents/")
                || path.startsWith("/uploads/w9/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {

            // Allow admins to operate regardless.
            if (user.getRole() != null && user.getRole().name().equals("ROLE_ADMIN")) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!user.isEmailVerified()) {
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Please verify your email address\",\"status\":403}");
                return;
            }

            if (!user.isAdminApproved()) {
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Account pending admin approval\",\"status\":403}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
