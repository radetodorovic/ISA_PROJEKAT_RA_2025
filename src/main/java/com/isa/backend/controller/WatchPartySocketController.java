package com.isa.backend.controller;

import com.isa.backend.dto.WatchPartyStartDTO;
import com.isa.backend.service.WatchPartyRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class WatchPartySocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private WatchPartyRoomService watchPartyRoomService;

    @MessageMapping("/watch-party/{roomId}/start")
    public void start(@DestinationVariable String roomId, WatchPartyStartDTO payload) {
        if (!watchPartyRoomService.exists(roomId)) {
            return;
        }
        WatchPartyStartDTO message = payload != null ? payload : new WatchPartyStartDTO();
        message.setRoomId(roomId);
        message.setStartedAt(LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/watch-party/" + roomId + "/start", message);
    }
}
