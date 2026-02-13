package com.isa.backend.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveUserTracker {
    private static final Duration ACTIVE_NOW_WINDOW = Duration.ofMinutes(5);
    private static final Duration ACTIVE_24H_WINDOW = Duration.ofHours(24);

    private final ConcurrentHashMap<String, Long> lastSeenByUser = new ConcurrentHashMap<>();

    public ActiveUserTracker(MeterRegistry registry) {
        Gauge.builder("app_active_users_now", this, ActiveUserTracker::countActiveNow)
                .description("Users active within the last 5 minutes")
                .register(registry);
        Gauge.builder("app_active_users_24h", this, ActiveUserTracker::countActive24h)
                .description("Users active within the last 24 hours")
                .register(registry);
    }

    public void recordActivity(String userKey) {
        lastSeenByUser.put(userKey, System.currentTimeMillis());
    }

    public void cleanupOldEntries() {
        long cutoff = System.currentTimeMillis() - ACTIVE_24H_WINDOW.toMillis();
        for (Map.Entry<String, Long> entry : lastSeenByUser.entrySet()) {
            if (entry.getValue() < cutoff) {
                lastSeenByUser.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduledCleanup() {
        cleanupOldEntries();
    }

    private double countActiveNow() {
        return countWithinWindow(ACTIVE_NOW_WINDOW);
    }

    private double countActive24h() {
        return countWithinWindow(ACTIVE_24H_WINDOW);
    }

    private long countWithinWindow(Duration window) {
        long cutoff = System.currentTimeMillis() - window.toMillis();
        return lastSeenByUser.values().stream().filter(ts -> ts >= cutoff).count();
    }
}
