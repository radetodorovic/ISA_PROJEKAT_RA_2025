package com.isa.backend.controller;

import com.isa.backend.dto.UserProfileDTO;
import com.isa.backend.model.User;
import com.isa.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfileById(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            UserProfileDTO dto = toDTO(user);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserProfileByUsername(@PathVariable String username) {
        try {
            User user = userService.findByUsername(username);
            UserProfileDTO dto = toDTO(user);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private UserProfileDTO toDTO(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setAddress(user.getAddress());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}

