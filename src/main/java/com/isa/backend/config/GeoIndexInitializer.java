package com.isa.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GeoIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GeoIndexInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.geo.init:true}")
    private boolean enabled;

    public GeoIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis");
        } catch (Exception ex) {
            log.warn("PostGIS extension is not available or permission denied: {}", ex.getMessage());
            return;
        }

        try {
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_video_posts_geo " +
                            "ON video_posts USING GIST " +
                            "(geography(ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)))"
            );
        } catch (Exception ex) {
            log.warn("Failed to create geo index: {}", ex.getMessage());
        }
    }
}
