package com.isa.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isa.backend.dto.UploadEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;

/**
 * Servis za merenje i poređenje performansi JSON vs Protobuf
 */
@Service
public class MessageComparisonService {

    private static final Logger logger = LoggerFactory.getLogger(MessageComparisonService.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProtobufSerializationService protobufService;

    // Liste za čuvanje vremena serijalizacije/deserijalizacije
    private final List<Long> jsonSerializationTimes = new ArrayList<>();
    private final List<Long> jsonDeserializationTimes = new ArrayList<>();
    private final List<Long> protobufSerializationTimes = new ArrayList<>();
    private final List<Long> protobufDeserializationTimes = new ArrayList<>();
    private final List<Integer> jsonMessageSizes = new ArrayList<>();
    private final List<Integer> protobufMessageSizes = new ArrayList<>();

    /**
     * Meri vreme i veličinu JSON serijalizacije
     */
    public byte[] serializeJsonAndMeasure(UploadEventDTO event) {
        try {
            long startTime = System.nanoTime();
            byte[] jsonBytes = objectMapper.writeValueAsBytes(event);
            long endTime = System.nanoTime();

            long serializationTime = endTime - startTime;

            synchronized (jsonSerializationTimes) {
                jsonSerializationTimes.add(serializationTime);
                jsonMessageSizes.add(jsonBytes.length);
            }

            logger.debug("JSON serijalizacija: {} ns, veličina: {} bytes",
                    serializationTime, jsonBytes.length);

            return jsonBytes;
        } catch (Exception e) {
            logger.error("Greška pri JSON serijalizaciji", e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * Meri vreme i veličinu Protobuf serijalizacije
     */
    public byte[] serializeProtobufAndMeasure(UploadEventDTO event) {
        try {
            long startTime = System.nanoTime();
            byte[] protobufBytes = protobufService.serializeToProtobuf(event);
            long endTime = System.nanoTime();

            long serializationTime = endTime - startTime;

            synchronized (protobufSerializationTimes) {
                protobufSerializationTimes.add(serializationTime);
                protobufMessageSizes.add(protobufBytes.length);
            }

            logger.debug("Protobuf serijalizacija: {} ns, veličina: {} bytes",
                    serializationTime, protobufBytes.length);

            return protobufBytes;
        } catch (Exception e) {
            logger.error("Greška pri Protobuf serijalizaciji", e);
            throw new RuntimeException("Protobuf serialization failed", e);
        }
    }

    /**
     * Beleži vreme JSON deserijalizacije (poziva se iz Consumer-a)
     */
    public void recordJsonDeserialization(long timeNanos) {
        synchronized (jsonDeserializationTimes) {
            jsonDeserializationTimes.add(timeNanos);
        }
    }

    /**
     * Beleži vreme Protobuf deserijalizacije (poziva se iz Consumer-a)
     */
    public void recordProtobufDeserialization(long timeNanos) {
        synchronized (protobufDeserializationTimes) {
            protobufDeserializationTimes.add(timeNanos);
        }
    }

    /**
     * Generiše izveštaj o poređenju performansi
     */
    public String generateComparisonReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n========================================\n");
        report.append("   JSON vs PROTOBUF - POREĐENJE\n");
        report.append("========================================\n\n");

        // JSON statistike
        report.append("JSON STATISTIKA:\n");
        report.append("----------------\n");
        appendStatistics(report, "Serijalizacija", jsonSerializationTimes);
        appendStatistics(report, "Deserijalizacija", jsonDeserializationTimes);
        appendSizeStatistics(report, "Veličina poruke", jsonMessageSizes);
        report.append("\n");

        // Protobuf statistike
        report.append("PROTOBUF STATISTIKA:\n");
        report.append("--------------------\n");
        appendStatistics(report, "Serijalizacija", protobufSerializationTimes);
        appendStatistics(report, "Deserijalizacija", protobufDeserializationTimes);
        appendSizeStatistics(report, "Veličina poruke", protobufMessageSizes);
        report.append("\n");

        // Poređenje
        report.append("POREĐENJE:\n");
        report.append("----------\n");
        compareMetrics(report);

        report.append("========================================\n");

        return report.toString();
    }

    private void appendStatistics(StringBuilder report, String metricName, List<Long> times) {
        if (times.isEmpty()) {
            report.append(String.format("%s: Nema podataka\n", metricName));
            return;
        }

        LongSummaryStatistics stats = times.stream()
                .mapToLong(Long::longValue)
                .summaryStatistics();

        report.append(String.format("%s:\n", metricName));
        report.append(String.format("  Broj merenja: %d\n", stats.getCount()));
        report.append(String.format("  Prosečno vreme: %.2f μs (%.2f ms)\n",
                stats.getAverage() / 1000.0, stats.getAverage() / 1_000_000.0));
        report.append(String.format("  Min vreme: %.2f μs\n", stats.getMin() / 1000.0));
        report.append(String.format("  Max vreme: %.2f μs\n", stats.getMax() / 1000.0));
    }

    private void appendSizeStatistics(StringBuilder report, String metricName, List<Integer> sizes) {
        if (sizes.isEmpty()) {
            report.append(String.format("%s: Nema podataka\n", metricName));
            return;
        }

        LongSummaryStatistics stats = sizes.stream()
                .mapToLong(Integer::longValue)
                .summaryStatistics();

        report.append(String.format("%s:\n", metricName));
        report.append(String.format("  Broj merenja: %d\n", stats.getCount()));
        report.append(String.format("  Prosečna veličina: %.2f bytes (%.2f KB)\n",
                stats.getAverage(), stats.getAverage() / 1024.0));
        report.append(String.format("  Min veličina: %d bytes\n", stats.getMin()));
        report.append(String.format("  Max veličina: %d bytes\n", stats.getMax()));
    }

    private void compareMetrics(StringBuilder report) {
        // Poređenje serijalizacije
        if (!jsonSerializationTimes.isEmpty() && !protobufSerializationTimes.isEmpty()) {
            double jsonAvgSer = jsonSerializationTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            double protobufAvgSer = protobufSerializationTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            double serDiff = ((jsonAvgSer - protobufAvgSer) / jsonAvgSer) * 100;
            report.append(String.format("Serijalizacija: Protobuf je %.2f%% %s od JSON\n",
                    Math.abs(serDiff), serDiff > 0 ? "brži" : "sporiji"));
        }

        // Poređenje deserijalizacije
        if (!jsonDeserializationTimes.isEmpty() && !protobufDeserializationTimes.isEmpty()) {
            double jsonAvgDeser = jsonDeserializationTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            double protobufAvgDeser = protobufDeserializationTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            double deserDiff = ((jsonAvgDeser - protobufAvgDeser) / jsonAvgDeser) * 100;
            report.append(String.format("Deserijalizacija: Protobuf je %.2f%% %s od JSON\n",
                    Math.abs(deserDiff), deserDiff > 0 ? "brži" : "sporiji"));
        }

        // Poređenje veličine
        if (!jsonMessageSizes.isEmpty() && !protobufMessageSizes.isEmpty()) {
            double jsonAvgSize = jsonMessageSizes.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
            double protobufAvgSize = protobufMessageSizes.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);

            double sizeDiff = ((jsonAvgSize - protobufAvgSize) / jsonAvgSize) * 100;
            report.append(String.format("Veličina poruke: Protobuf je %.2f%% %s od JSON\n",
                    Math.abs(sizeDiff), sizeDiff > 0 ? "manji" : "veći"));
        }
    }

    /**
     * Resetuje sve statistike
     */
    public void resetStatistics() {
        synchronized (jsonSerializationTimes) {
            jsonSerializationTimes.clear();
            jsonDeserializationTimes.clear();
            jsonMessageSizes.clear();
        }
        synchronized (protobufSerializationTimes) {
            protobufSerializationTimes.clear();
            protobufDeserializationTimes.clear();
            protobufMessageSizes.clear();
        }
        logger.info("Statistike resetovane");
    }

    /**
     * Vraća broj zabeleženih merenja
     */
    public int getJsonMeasurementCount() {
        return jsonSerializationTimes.size();
    }

    public int getProtobufMeasurementCount() {
        return protobufSerializationTimes.size();
    }
}

