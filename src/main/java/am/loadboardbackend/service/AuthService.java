package am.loadboardbackend.service;

import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.UserRepository;
import am.loadboardbackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return new LoginResponse(jwtUtil.generateToken(user));
    }

    public am.loadboardbackend.model.User currentUserOrThrow() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof am.loadboardbackend.model.User user) {
            return user;
        }
        throw new RuntimeException("No authenticated user");
    }
}
