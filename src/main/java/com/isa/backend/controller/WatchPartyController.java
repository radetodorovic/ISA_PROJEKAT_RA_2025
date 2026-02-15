package com.isa.backend.controller;

import com.isa.backend.dto.WatchPartyRoomDTO;
import com.isa.backend.model.User;
import com.isa.backend.service.UserService;
import com.isa.backend.service.WatchPartyRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/watch-party")
@CrossOrigin(origins = "*")
public class WatchPartyController {

    @Autowired
    private WatchPartyRoomService watchPartyRoomService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> createRoom(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Morate biti prijavljeni da biste kreirali watch party sobu.");
        }
        User user = userService.findByEmail(principal.getName());
        WatchPartyRoomService.Room room = watchPartyRoomService.createRoom(user.getId());

        WatchPartyRoomDTO dto = new WatchPartyRoomDTO();
        dto.setRoomId(room.getRoomId());
        dto.setHostUserId(room.getHostUserId());
        dto.setCreatedAt(room.getCreatedAt());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {
        WatchPartyRoomService.Room room = watchPartyRoomService.getRoom(roomId);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Watch party soba nije pronađena.");
        }
        WatchPartyRoomDTO dto = new WatchPartyRoomDTO();
        dto.setRoomId(room.getRoomId());
        dto.setHostUserId(room.getHostUserId());
        dto.setCreatedAt(room.getCreatedAt());
        return ResponseEntity.ok(dto);
    }
}
