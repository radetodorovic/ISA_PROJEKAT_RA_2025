package com.isa.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 1;

    // IP adresa -> broj pokušaja
    private Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();

    // IP adresa -> vreme prvog pokušaja
    private Map<String, LocalDateTime> firstAttemptTime = new ConcurrentHashMap<>();

    public void loginFailed(String ipAddress) {
        int attempts = attemptsCache.getOrDefault(ipAddress, 0);

        if (attempts == 0) {
            firstAttemptTime.put(ipAddress, LocalDateTime.now());
        }

        attemptsCache.put(ipAddress, attempts + 1);
    }

    public void loginSucceeded(String ipAddress) {
        attemptsCache.remove(ipAddress);
        firstAttemptTime.remove(ipAddress);
    }

    public boolean isBlocked(String ipAddress) {
        if (!attemptsCache.containsKey(ipAddress)) {
            return false;
        }

        LocalDateTime firstAttempt = firstAttemptTime.get(ipAddress);
        LocalDateTime now = LocalDateTime.now();

        // Ako je prošlo više od 1 minuta, resetuj pokušaje
        if (firstAttempt.plusMinutes(BLOCK_DURATION_MINUTES).isBefore(now)) {
            attemptsCache.remove(ipAddress);
            firstAttemptTime.remove(ipAddress);
            return false;
        }

        return attemptsCache.get(ipAddress) >= MAX_ATTEMPTS;
    }
}