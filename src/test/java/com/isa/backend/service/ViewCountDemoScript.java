package com.isa.backend.service;

import com.isa.backend.model.VideoPost;
import com.isa.backend.repository.VideoPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Skripta za demonstraciju thread-safe brojača pregleda videa.
 * Simulira istovremenu posetu istom videu od strane 100 korisnika.
 *
 * Pokretanje: mvn spring-boot:run -Dspring-boot.run.main-class=com.isa.backend.service.ViewCountDemoScript
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.isa.backend")
public class ViewCountDemoScript {

    public static void main(String[] args) {
        SpringApplication.run(ViewCountDemoScript.class, args);
    }

    @Bean
    public CommandLineRunner demoRunner(
            @Autowired VideoPostRepository videoPostRepository,
            @Autowired VideoPostService videoPostService) {

        return args -> {
            System.out.println("\n");
            System.out.println("╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║  DEMONSTRACIJA: Thread-Safe Brojač Pregleda Videa             ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝");
            System.out.println();

            // Kreiranje test videa
            VideoPost testVideo = new VideoPost();
            testVideo.setTitle("Test Video - Konkurentni Pristup");
            testVideo.setDescription("Demonstracija thread-safe brojača pregleda");
            testVideo.setTags(new HashSet<>());
            testVideo.setThumbnailPath("demo-thumbnail.jpg");
            testVideo.setVideoPath("demo-video-" + System.currentTimeMillis() + ".mp4");
            testVideo.setVideoSize(1024L);
            testVideo.setUserId(1L);
            testVideo.setViewCount(0);

            testVideo = videoPostRepository.save(testVideo);
            System.out.println("✓ Kreiran test video sa ID: " + testVideo.getId());
            System.out.println("✓ Početni broj pregleda: " + testVideo.getViewCount());
            System.out.println();

            // Simulacija istovremenih pristupa
            int numberOfConcurrentUsers = 100;
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("SIMULACIJA: " + numberOfConcurrentUsers + " korisnika istovremeno pristupa videu");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println();

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(numberOfConcurrentUsers);
            ExecutorService executor = Executors.newFixedThreadPool(20);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            final Long videoId = testVideo.getId();
            final String videoPath = testVideo.getVideoPath();

            long startTime = System.currentTimeMillis();

            // Pokretanje konkurentnih thread-ova
            for (int i = 1; i <= numberOfConcurrentUsers; i++) {
                final int userId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        // Inkrement view count-a (thread-safe operacija)
                        videoPostService.incrementViewCountByPath(videoPath);

                        int count = successCount.incrementAndGet();
                        if (count % 10 == 0) {
                            System.out.println("  → " + count + " korisnika uspešno pristupilo");
                        }

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("  ✗ Korisnik #" + userId + " - Greška: " + e.getMessage());
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            System.out.println("Pokretanje svih thread-ova istovremeno...");
            startLatch.countDown();

            // Čekanje da svi završe
            boolean finished = endLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            System.out.println();
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("REZULTATI:");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Osvježavanje podataka iz baze
            VideoPost updatedVideo = videoPostRepository.findById(videoId).orElseThrow();

            System.out.println();
            System.out.println("  Vreme izvršavanja:        " + duration + " ms");
            System.out.println("  Uspešni pristupи:         " + successCount.get() + " / " + numberOfConcurrentUsers);
            System.out.println("  Greške:                   " + errorCount.get());
            System.out.println();
            System.out.println("  Očekivani broj pregleda:  " + numberOfConcurrentUsers);
            System.out.println("  Stvarni broj pregleda:    " + updatedVideo.getViewCount());
            System.out.println();

            boolean success = (updatedVideo.getViewCount() == numberOfConcurrentUsers);

            if (success) {
                System.out.println("  ╔═══════════════════════════════════════════════════════════╗");
                System.out.println("  ║  ✓ TEST USPEŠAN! Brojač je konzistentan!                 ║");
                System.out.println("  ║    Atomski UPDATE na nivou baze garantuje thread-safety  ║");
                System.out.println("  ╚═══════════════════════════════════════════════════════════╝");
            } else {
                System.out.println("  ✗ GREŠKA! Brojač nije konzistentan!");
                System.out.println("    Razlika: " + (numberOfConcurrentUsers - updatedVideo.getViewCount()));
            }

            System.out.println();
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println();

            // Čišćenje
            videoPostRepository.delete(updatedVideo);
            System.out.println("✓ Test video obrisan iz baze");
            System.out.println();

            System.exit(0);
        };
    }
}

