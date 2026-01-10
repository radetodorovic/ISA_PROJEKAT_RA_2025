package com.isa.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.InetAddress;
import java.net.URI;

@SpringBootApplication
@EnableCaching
public class BackendApplication {

    private static final Logger log = LoggerFactory.getLogger(BackendApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BackendApplication.class);
        Environment env = app.run(args).getEnvironment();

        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        if (contextPath == null || contextPath.equals("/")) {
            contextPath = "";
        }

        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.debug("Ne mogu da dohvatim lokalnu IP adresu, koristi se localhost: {}", e.getMessage());
        }

        String localUrl = "http://localhost:" + port + contextPath;
        String externalUrl = "http://" + hostAddress + ":" + port + contextPath;

        log.info("\n----------------------------------------------------------\n" +
                "Application '{}' is running! Access URLs:\n" +
                "Local: \t\t{}\n" +
                "External: \t{}\n" +
                "----------------------------------------------------------",
                env.getProperty("spring.application.name", "application"), localUrl, externalUrl);

        // Pokušaj da automatski otvorimo browser (samo ako je moguće)
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(localUrl));
            }
        } catch (Exception ex) {
            log.debug("Neuspešno otvaranje browsera: {}", ex.getMessage());
        }
    }
}