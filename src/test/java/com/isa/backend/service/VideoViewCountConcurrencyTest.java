package com.isa.backend.service;

import com.isa.backend.model.VideoPost;
import com.isa.backend.repository.VideoPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test za demonstraciju thread-safe inkrementiranja brojača pregleda videa.
 * Simulira istovremenu posetu istom videu od strane više korisnika.
 */
@SpringBootTest
@ActiveProfiles("test")
public class VideoViewCountConcurrencyTest {

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Autowired
    private VideoPostService videoPostService;

    private VideoPost testVideo;

    @BeforeEach
    public void setup() {
        // Očisti bazu i kreiraj test video
        videoPostRepository.deleteAll();

        testVideo = new VideoPost();
        testVideo.setTitle("Test Video - Concurrency");
        testVideo.setDescription("Video za testiranje konkurentnih pristupa");
        testVideo.setTags(new HashSet<>());
        testVideo.setThumbnailPath("test-thumbnail.jpg");
        testVideo.setVideoPath("test-video.mp4");
        testVideo.setVideoSize(1024L);
        testVideo.setUserId(1L);
        testVideo.setViewCount(0);

        testVideo = videoPostRepository.save(testVideo);
    }

    @Test
    public void testConcurrentViewCountIncrement() throws InterruptedException {
        // Broj istovremenih korisnika koji pristupaju videu
        int numberOfUsers = 100;

        // CountDownLatch osigurava da svi thread-ovi počnu u isto vreme
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfUsers);

        // Thread pool za simulaciju više korisnika
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Brojač uspešnih inkremenata
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        System.out.println("=================================================");
        System.out.println("POČETAK TESTA: Istovremena poseta videu");
        System.out.println("=================================================");
        System.out.println("Broj korisnika: " + numberOfUsers);
        System.out.println("Početni view count: " + testVideo.getViewCount());
        System.out.println("-------------------------------------------------");

        long startTime = System.currentTimeMillis();

        // Kreiraj thread-ove koji simuliraju korisnike
        for (int i = 0; i < numberOfUsers; i++) {
            final int userId = i + 1;
            executor.submit(() -> {
                try {
                    // Sačekaj signal za početak
                    startLatch.await();

                    // Simuliraj posetu videu - inkrement view count-a
                    videoPostService.incrementViewCountByPath(testVideo.getVideoPath());
                    successCount.incrementAndGet();

                    System.out.println("Korisnik #" + userId + " uspešno pristupio videu");

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Korisnik #" + userId + " - GREŠKA: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Pokreni sve thread-ove istovremeno
        System.out.println("Pokretanje istovremenih pristupa...");
        startLatch.countDown();

        // Sačekaj da svi thread-ovi završe (maksimalno 30 sekundi)
        boolean finished = endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("-------------------------------------------------");
        System.out.println("Vreme izvršavanja: " + duration + " ms");
        System.out.println("Uspešni pristupи: " + successCount.get());
        System.out.println("Greške: " + errorCount.get());

        // Proveri rezultate
        assertTrue(finished, "Test nije završen u predviđenom vremenu");
        assertEquals(0, errorCount.get(), "Ne bi trebalo biti grešaka");

        // Osvježi podatke iz baze
        VideoPost updatedVideo = videoPostRepository.findById(testVideo.getId()).orElseThrow();

        System.out.println("-------------------------------------------------");
        System.out.println("REZULTAT:");
        System.out.println("Očekivani view count: " + numberOfUsers);
        System.out.println("Stvarni view count: " + updatedVideo.getViewCount());
        System.out.println("=================================================");

        // KLJUČNA PROVERA: View count mora biti tačno jednak broju korisnika
        assertEquals(numberOfUsers, updatedVideo.getViewCount(),
            "View count mora biti konzistentan - svaki pristup tačno jednom povećava brojač");
    }

    @Test
    public void testConcurrentViewCountIncrementById() throws InterruptedException {
        int numberOfUsers = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfUsers);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);

        System.out.println("=================================================");
        System.out.println("TEST: Inkrement po ID-u (istovremeni pristup)");
        System.out.println("=================================================");

        for (int i = 0; i < numberOfUsers; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    videoPostService.incrementViewCountById(testVideo.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Greška: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        VideoPost updatedVideo = videoPostRepository.findById(testVideo.getId()).orElseThrow();

        System.out.println("Uspešni pristupи: " + successCount.get());
        System.out.println("View count: " + updatedVideo.getViewCount());
        System.out.println("=================================================");

        assertEquals(numberOfUsers, updatedVideo.getViewCount(),
            "View count mora biti tačan i pri pristupu po ID-u");
    }

    @Test
    public void testSequentialViewCountIncrement() {
        System.out.println("=================================================");
        System.out.println("TEST: Sekvencijalni pristup (kontrolna provera)");
        System.out.println("=================================================");

        int numberOfViews = 10;
        for (int i = 0; i < numberOfViews; i++) {
            videoPostService.incrementViewCountByPath(testVideo.getVideoPath());
            System.out.println("Pristup #" + (i + 1) + " evidentiran");
        }

        VideoPost updatedVideo = videoPostRepository.findById(testVideo.getId()).orElseThrow();
        System.out.println("Finalni view count: " + updatedVideo.getViewCount());
        System.out.println("=================================================");

        assertEquals(numberOfViews, updatedVideo.getViewCount(),
            "Sekvencijalni pristup mora biti tačan");
    }
}

