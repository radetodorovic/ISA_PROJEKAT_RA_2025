package com.isa.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class WelcomeController {

    @GetMapping("/")
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "ISA Backend API - Video Sharing Platform");
        response.put("status", "running");
        response.put("endpoints", Map.of(
                "auth", Map.of(
                        "register", "POST /api/auth/register",
                        "login", "POST /api/auth/login"
                ),
                "videos", Map.of(
                        "getAll", "GET /api/videos",
                        "getById", "GET /api/videos/{id}",
                        "upload", "POST /api/videos (requires auth)",
                        "stream", "GET /api/videos/stream/{videoId}",
                        "thumbnail", "GET /api/videos/thumbnail/{videoId}"
                ),
                "users", Map.of(
                        "getProfile", "GET /api/users/{id}",
                        "updateProfile", "PUT /api/users/{id} (requires auth)"
                ),
                "comments", Map.of(
                        "add", "POST /api/videos/{videoId}/comments (requires auth)",
                        "get", "GET /api/videos/{videoId}/comments"
                )
        ));
        return response;
    }
}

