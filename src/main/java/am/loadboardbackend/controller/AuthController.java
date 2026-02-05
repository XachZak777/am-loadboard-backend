package am.loadboardbackend.controller;

import am.loadboardbackend.dto.LoginRequest;
import am.loadboardbackend.dto.LoginResponse;
import am.loadboardbackend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        String token = authService.login(req.getEmail(), req.getPassword());
        return new LoginResponse(token);
    }
}
