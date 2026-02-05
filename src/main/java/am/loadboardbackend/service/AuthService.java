package am.loadboardbackend.service;

import am.loadboardbackend.model.User;
import am.loadboardbackend.repository.UserRepository;
import am.loadboardbackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public String login(String email, String password) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("INVALID_CREDENTIALS"));

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("INVALID_CREDENTIALS");
        }

        return jwtUtil.generateToken(user);
    }
}
