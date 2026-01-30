package com.isa.backend.controller;

import com.isa.backend.dto.GeoLocationDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GeoIpController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/geoip")
    public ResponseEntity<GeoLocationDto> getGeoForRequest(HttpServletRequest request) {
        String ip = extractClientIp(request);
        try {
            String url = "http://ip-api.com/json/" + ip + "?fields=status,message,lat,lon";
            Map<?, ?> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && "success".equalsIgnoreCase(String.valueOf(resp.get("status")))) {
                Double lat = resp.get("lat") != null ? ((Number) resp.get("lat")).doubleValue() : null;
                Double lon = resp.get("lon") != null ? ((Number) resp.get("lon")).doubleValue() : null;
                if (lat != null && lon != null) {
                    return ResponseEntity.ok(new GeoLocationDto(lat, lon));
                } else {
                    return ResponseEntity.badRequest().body(new GeoLocationDto("No coordinates returned"));
                }
            } else {
                String msg = resp != null ? String.valueOf(resp.get("message")) : "Unknown error";
                return ResponseEntity.badRequest().body(new GeoLocationDto(msg));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new GeoLocationDto("Error looking up IP: " + e.getMessage()));
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

