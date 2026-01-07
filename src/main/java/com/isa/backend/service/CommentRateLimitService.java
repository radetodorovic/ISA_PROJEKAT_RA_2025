package com.isa.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Service that enforces per-user comment rate limits.
 * Allows up to MAX_COMMENTS_PER_WINDOW comments per WINDOW_MINUTES sliding window.
 */
@Service
public class CommentRateLimitService {

    private static final int MAX_COMMENTS_PER_WINDOW = 60;
    private static final int WINDOW_MINUTES = 60;

    // userId -> deque of comment timestamps (ordered oldest -> newest)
    private final Map<Long, Deque<LocalDateTime>> userComments = new ConcurrentHashMap<>();

    /**
     * Attempts to record a comment for the given user. Returns true if allowed, false if rate limit exceeded.
     * This method uses a per-user deque and prunes entries older than the sliding window.
     */
    public boolean tryConsume(Long userId) {
        if (userId == null) return false;
        LocalDateTime now = LocalDateTime.now();
        Deque<LocalDateTime> deque = userComments.computeIfAbsent(userId, id -> new ConcurrentLinkedDeque<>());

        synchronized (deque) {
            // prune old timestamps
            LocalDateTime threshold = now.minus(WINDOW_MINUTES, ChronoUnit.MINUTES);
            while (!deque.isEmpty() && deque.peekFirst().isBefore(threshold)) {
                deque.pollFirst();
            }

            if (deque.size() >= MAX_COMMENTS_PER_WINDOW) {
                return false;
            }

            // record this comment timestamp
            deque.addLast(now);
            return true;
        }
    }

    /**
     * Returns the number of comments the user has made in the current window.
     */
    public int countInWindow(Long userId) {
        Deque<LocalDateTime> deque = userComments.get(userId);
        if (deque == null) return 0;
        synchronized (deque) {
            LocalDateTime threshold = LocalDateTime.now().minus(WINDOW_MINUTES, ChronoUnit.MINUTES);
            while (!deque.isEmpty() && deque.peekFirst().isBefore(threshold)) {
                deque.pollFirst();
            }
            return deque.size();
        }
    }

    /**
     * For testing or admin purposes: resets counters for a user.
     */
    public void reset(Long userId) {
        userComments.remove(userId);
    }
}

