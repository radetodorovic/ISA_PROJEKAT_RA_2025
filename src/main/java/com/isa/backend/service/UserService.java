package com.isa.backend.service;

import com.isa.backend.dto.RegisterRequest;
import com.isa.backend.model.User;
import com.isa.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Transactional
    public User registerUser(RegisterRequest request) {
        // Validacija - da li email već postoji
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email već postoji");
        }

        // Validacija - da li username već postoji
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Korisničko ime već postoji");
        }

        // Validacija - da li se lozinke poklapaju
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Lozinke se ne poklapaju");
        }

        // Kreiranje novog korisnika
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAddress(request.getAddress());
        user.setEnabled(false);

        // Generisanje aktivacionog tokena
        String activationToken = UUID.randomUUID().toString();
        user.setActivationToken(activationToken);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));

        // Čuvanje korisnika
        User savedUser = userRepository.save(user);

        // Slanje aktivacionog email-a
        String activationLink = "http://localhost:8080/api/auth/activate?token=" + activationToken;
        emailService.sendActivationEmail(user.getEmail(), activationLink);

        return savedUser;
    }

    public boolean activateAccount(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new RuntimeException("Nevažeći aktivacioni token"));

        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Aktivacioni token je istekao");
        }

        user.setEnabled(true);
        user.setActivationToken(null);
        user.setTokenExpiryDate(null);
        userRepository.save(user);

        return true;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Korisnik nije pronađen"));
    }
}