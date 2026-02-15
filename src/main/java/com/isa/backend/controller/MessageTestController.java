package com.isa.backend.controller;

import com.isa.backend.dto.UploadEventDTO;
import com.isa.backend.service.MessageComparisonService;
import com.isa.backend.service.VideoUploadEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Test Controller za generisanje test poruka
 * Koristi se za brzo testiranje performansi bez potrebe da se upload-uju stvarni videi
 */
@RestController
@RequestMapping("/api/message-test")
@CrossOrigin(origins = "*")
public class MessageTestController {

    @Autowired
    private VideoUploadEventProducer uploadEventProducer;

    @Autowired
    private MessageComparisonService comparisonService;

    private final Random random = new Random();

    /**
     * POST /api/message-test/generate/{count}
     * Generiše i šalje test poruke (bez stvarnog upload-a videa)
     */
    @PostMapping("/generate/{count}")
    public ResponseEntity<Map<String, Object>> generateTestMessages(@PathVariable int count) {
        if (count < 1 || count > 1000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Count mora biti između 1 i 1000"));
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            try {
                UploadEventDTO testEvent = generateRandomEvent(i);

                // Meri i šalji JSON
                comparisonService.serializeJsonAndMeasure(testEvent);
                uploadEventProducer.sendJsonEvent(testEvent);

                // Meri i šalji Protobuf (dummy)
                byte[] protobufBytes = comparisonService.serializeProtobufAndMeasure(testEvent);
                uploadEventProducer.sendProtobufEvent(protobufBytes);

                successCount++;
            } catch (Exception e) {
                errors.add("Greška pri generisanju poruke " + i + ": " + e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("requested", count);
        response.put("sent", successCount);
        response.put("jsonMeasurements", comparisonService.getJsonMeasurementCount());
        response.put("protobufMeasurements", comparisonService.getProtobufMeasurementCount());

        if (!errors.isEmpty()) {
            response.put("errors", errors);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Generiše random test event
     */
    private UploadEventDTO generateRandomEvent(int index) {
        List<String> sampleTitles = Arrays.asList(
                "Najbolji gameplay ever!",
                "Tutorial za početnike",
                "Vlog iz Beograda",
                "Cooking show - Srpska kuhinja",
                "Tech review 2025",
                "Gaming highlights",
                "Travel vlog Serbia",
                "Music performance live"
        );

        List<String> sampleDescriptions = Arrays.asList(
                "Ovo je opis videa koji sam uploadovao.",
                "Pogledajte ovaj neverovatni sadržaj!",
                "Najbolji video do sada na kanalu.",
                "Ne propustite da pogledate do kraja!",
                "Lajkujte i subscribe-ujte se!"
        );

        List<String> sampleLocations = Arrays.asList(
                "Beograd",
                "Novi Sad",
                "Niš",
                "Kragujevac",
                "Subotica"
        );

        List<List<String>> sampleTags = Arrays.asList(
                Arrays.asList("gaming", "tutorial", "serbia"),
                Arrays.asList("vlog", "travel", "2025"),
                Arrays.asList("food", "cooking", "recipe"),
                Arrays.asList("tech", "review", "gadgets"),
                Arrays.asList("music", "live", "performance")
        );

        // Random lokacije u Srbiji
        double[] latitudes = {44.8176, 45.2671, 43.3209, 44.0165, 46.1005};
        double[] longitudes = {20.4574, 19.8335, 21.8954, 20.9114, 19.6647};

        int locationIndex = random.nextInt(sampleLocations.size());

        return UploadEventDTO.builder()
                .videoId("test-video-" + index)
                .title(sampleTitles.get(random.nextInt(sampleTitles.size())) + " #" + index)
                .description(sampleDescriptions.get(random.nextInt(sampleDescriptions.size())))
                .tags(sampleTags.get(random.nextInt(sampleTags.size())))
                .authorEmail("test.user" + (random.nextInt(10) + 1) + "@example.com")
                .authorName("Test User " + (random.nextInt(10) + 1))
                .videoSizeBytes((long) (random.nextInt(100) + 10) * 1024 * 1024) // 10-110 MB
                .thumbnailSizeBytes((long) (random.nextInt(500) + 100) * 1024) // 100-600 KB
                .videoPath("test-videos/" + UUID.randomUUID() + ".mp4")
                .thumbnailPath("test-thumbnails/" + UUID.randomUUID() + ".jpg")
                .uploadTimestamp(System.currentTimeMillis())
                .location(sampleLocations.get(locationIndex))
                .latitude(latitudes[locationIndex])
                .longitude(longitudes[locationIndex])
                .transcodeProfiles(Arrays.asList("360p", "720p", "1080p"))
                .build();
    }
}

