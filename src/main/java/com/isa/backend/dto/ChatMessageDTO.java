package com.isa.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private String username;
    private String message;
    private LocalDateTime timestamp;
    private MessageType type;

    public enum MessageType {
        CHAT,      // Obična chat poruka
        JOIN,      // Korisnik se pridružio
        LEAVE      // Korisnik je napustio chat
    }
}

