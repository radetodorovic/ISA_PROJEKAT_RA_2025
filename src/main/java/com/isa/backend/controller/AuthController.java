package com.isa.backend.controller;

import com.isa.backend.dto.AuthResponse;
import com.isa.backend.dto.LoginRequest;
import com.isa.backend.dto.RegisterRequest;
import com.isa.backend.model.User;
import com.isa.backend.security.JwtUtil;
import com.isa.backend.service.LoginAttemptService;
import com.isa.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            userService.registerUser(request);

            // Don't auto-login. Require email activation first.
            return ResponseEntity.ok("Registracija uspešna! Proverite svoj email i aktivirajte nalog.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/activate")
    public ResponseEntity<?> activateAccount(@RequestParam("token") String token) {
        try {
            userService.activateAccount(token);
            return ResponseEntity.ok("Nalog je uspešno aktiviran! Možete se prijaviti.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        try {
            String ipAddress = getClientIP(httpRequest);

            // Proveri da li je IP blokiran
            if (loginAttemptService.isBlocked(ipAddress)) {
                return ResponseEntity.status(429).body("Previše pokušaja prijave. Pokušajte ponovo za 1 minut.");
            }

            User user = userService.findByEmail(request.getEmail());

            if (!user.isEnabled()) {
                loginAttemptService.loginFailed(ipAddress);
                return ResponseEntity.badRequest().body("Nalog nije aktiviran. Proverite email.");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                loginAttemptService.loginFailed(ipAddress);
                return ResponseEntity.badRequest().body("Pogrešna lozinka");
            }

            // Uspešan login - resetuj pokušaje
            loginAttemptService.loginSucceeded(ipAddress);

            String token = jwtUtil.generateToken(user.getEmail());

            return ResponseEntity.ok(new AuthResponse(token, "Uspešna prijava!"));
        } catch (RuntimeException e) {
            String ipAddress = getClientIP(httpRequest);
            loginAttemptService.loginFailed(ipAddress);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}