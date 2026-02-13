package com.isa.backend.controller;

import com.isa.backend.dto.ChatMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{videoId}")
    public void sendMessage(@DestinationVariable String videoId, ChatMessageDTO message) {
        if (message == null) {
            return;
        }
        message.setTimestamp(LocalDateTime.now());
        message.setType(ChatMessageDTO.MessageType.CHAT);
        messagingTemplate.convertAndSend("/topic/video/" + videoId + "/chat", message);
    }

    @MessageMapping("/chat/{videoId}/join")
    public void join(@DestinationVariable String videoId, ChatMessageDTO message) {
        String username = message != null ? message.getUsername() : "Guest";
        ChatMessageDTO joinMsg = new ChatMessageDTO(
                username,
                username + " se pridruÅ¾io chatu.",
                LocalDateTime.now(),
                ChatMessageDTO.MessageType.JOIN
        );
        messagingTemplate.convertAndSend("/topic/video/" + videoId + "/chat", joinMsg);
    }
}
