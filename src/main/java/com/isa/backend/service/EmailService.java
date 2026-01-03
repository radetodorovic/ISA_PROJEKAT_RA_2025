package com.isa.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendActivationEmail(String to, String activationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Aktivacija naloga - ISA Projekat");
        message.setText("Poštovani,\n\n" +
                "Hvala što ste se registrovali!\n\n" +
                "Molimo vas da aktivirate svoj nalog klikom na sledeći link:\n" +
                activationLink + "\n\n" +
                "Link je važeći 24 sata.\n\n" +
                "Srdačan pozdrav,\n" +
                "ISA Tim");

        mailSender.send(message);
    }
}