package com.isa.backend.service;

import com.isa.backend.dto.RegisterRequest;
import com.isa.backend.model.User;
import com.isa.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

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

        // For production we require email verification: keep account disabled until activation
        user.setEnabled(false);

        // Generisanje aktivacionog tokena
        String activationToken = UUID.randomUUID().toString();
        user.setActivationToken(activationToken);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));

        // Čuvanje korisnika
        User savedUser = userRepository.save(user);

        String activationLink = "http://localhost:8080/api/auth/activate?token=" + activationToken;
        // Log activation link so it is easy to test locally
        log.info("Activation link for {}: {}", user.getEmail(), activationLink);

        // Pokušaj slanja aktivacionog email-a, ali ne aktiviramo nalog automatski ako ne uspe
        try {
            emailService.sendActivationEmail(user.getEmail(), activationLink);
        } catch (Exception e) {
            // Ako email servis nije konfigurisan, logujemo to — nalog ostaje neaktiviran
            log.warn("Email servis nije dostupan, nalog je kreiran, ali nije aktiviran: {}", e.getMessage());
        }

        return savedUser;
    }

    public void activateAccount(String token) {
        User user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new RuntimeException("Nevažeći aktivacioni token"));

        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Aktivacioni token je istekao");
        }

        user.setEnabled(true);
        user.setActivationToken(null);
        user.setTokenExpiryDate(null);
        userRepository.save(user);
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