package am.loadboardbackend.controller;

import am.loadboardbackend.dto.auth.LoginRequest;
import am.loadboardbackend.dto.auth.LoginResponse;
import am.loadboardbackend.dto.auth.RegisterAdminRequest;
import am.loadboardbackend.service.AuthService;
import am.loadboardbackend.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.getEmail(), request.getPassword());
    }

    @PostMapping("/register-admin")
    public ResponseEntity<LoginResponse> registerAdmin(@RequestBody RegisterAdminRequest request) {
        LoginResponse response = registrationService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
