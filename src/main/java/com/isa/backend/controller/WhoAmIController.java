package com.isa.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class WhoAmIController {

    @GetMapping("/whoami")
    public ResponseEntity<Map<String, String>> whoAmI() {
        String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
        return ResponseEntity.ok(Map.of("hostname", hostname));
    }
}
