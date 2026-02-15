package com.isa.backend.controller;

import com.isa.backend.service.MessageComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller za poređenje performansi JSON vs Protobuf
 */
@RestController
@RequestMapping("/api/message-comparison")
@CrossOrigin(origins = "*")
public class MessageComparisonController {

    @Autowired
    private MessageComparisonService comparisonService;

    /**
     * GET /api/message-comparison/report
     * Vraća izveštaj o poređenju performansi
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getComparisonReport() {
        String report = comparisonService.generateComparisonReport();

        Map<String, Object> response = new HashMap<>();
        response.put("report", report);
        response.put("jsonMeasurements", comparisonService.getJsonMeasurementCount());
        response.put("protobufMeasurements", comparisonService.getProtobufMeasurementCount());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/message-comparison/reset
     * Resetuje sve statistike
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetStatistics() {
        comparisonService.resetStatistics();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Statistike uspešno resetovane");

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/message-comparison/status
     * Vraća trenutni status merenja
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("jsonMeasurements", comparisonService.getJsonMeasurementCount());
        status.put("protobufMeasurements", comparisonService.getProtobufMeasurementCount());
        status.put("ready", comparisonService.getJsonMeasurementCount() >= 50 &&
                           comparisonService.getProtobufMeasurementCount() >= 50);

        return ResponseEntity.ok(status);
    }
}

