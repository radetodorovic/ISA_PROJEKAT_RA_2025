package com.isa.backend.controller;

import com.isa.backend.dto.AuthResponse;
import com.isa.backend.dto.LoginRequest;
import com.isa.backend.dto.RegisterRequest;
import com.isa.backend.model.User;
import com.isa.backend.security.JwtUtil;
import com.isa.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;  // ← OVO

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(request);
            return ResponseEntity.ok(new AuthResponse(null,
                    "Registracija uspešna! Proverite email za aktivacioni link."));
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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.findByEmail(request.getEmail());

            if (!user.isEnabled()) {
                return ResponseEntity.badRequest().body("Nalog nije aktiviran. Proverite email.");
            }

            if (!user.getPassword().equals(request.getPassword())) {
                return ResponseEntity.badRequest().body("Pogrešna lozinka");
            }

            String token = jwtUtil.generateToken(user.getEmail());

            return ResponseEntity.ok(new AuthResponse(token, "Uspešna prijava!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}