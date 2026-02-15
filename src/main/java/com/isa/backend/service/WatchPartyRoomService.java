package com.isa.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WatchPartyRoomService {

    public static class Room {
        private final String roomId;
        private final Long hostUserId;
        private final LocalDateTime createdAt;

        public Room(String roomId, Long hostUserId, LocalDateTime createdAt) {
            this.roomId = roomId;
            this.hostUserId = hostUserId;
            this.createdAt = createdAt;
        }

        public String getRoomId() {
            return roomId;
        }

        public Long getHostUserId() {
            return hostUserId;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public Room createRoom(Long hostUserId) {
        String roomId = generateRoomId();
        Room room = new Room(roomId, hostUserId, LocalDateTime.now());
        rooms.put(roomId, room);
        return room;
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public boolean exists(String roomId) {
        return rooms.containsKey(roomId);
    }

    private String generateRoomId() {
        String raw = UUID.randomUUID().toString().replace("-", "");
        return raw.substring(0, 8);
    }
}
