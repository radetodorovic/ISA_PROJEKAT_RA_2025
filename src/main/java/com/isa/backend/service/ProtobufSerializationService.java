package com.isa.backend.service;

import com.google.protobuf.ByteString;
import com.isa.backend.dto.UploadEventDTO;
import com.isa.backend.proto.UploadEventProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servis za konverziju između DTO i Protobuf formata
 */
@Service
public class ProtobufSerializationService {

    private static final Logger logger = LoggerFactory.getLogger(ProtobufSerializationService.class);

    /**
     * Konvertuje UploadEventDTO u Protobuf format i serijalizuje
     */
    public byte[] serializeToProtobuf(UploadEventDTO dto) {
        try {
            UploadEventProto.UploadEvent.Builder builder = UploadEventProto.UploadEvent.newBuilder()
                    .setVideoId(dto.getVideoId() != null ? dto.getVideoId() : "")
                    .setTitle(dto.getTitle() != null ? dto.getTitle() : "")
                    .setDescription(dto.getDescription() != null ? dto.getDescription() : "")
                    .setAuthorEmail(dto.getAuthorEmail() != null ? dto.getAuthorEmail() : "")
                    .setAuthorName(dto.getAuthorName() != null ? dto.getAuthorName() : "")
                    .setVideoSizeBytes(dto.getVideoSizeBytes() != null ? dto.getVideoSizeBytes() : 0L)
                    .setThumbnailSizeBytes(dto.getThumbnailSizeBytes() != null ? dto.getThumbnailSizeBytes() : 0L)
                    .setVideoPath(dto.getVideoPath() != null ? dto.getVideoPath() : "")
                    .setThumbnailPath(dto.getThumbnailPath() != null ? dto.getThumbnailPath() : "")
                    .setUploadTimestamp(dto.getUploadTimestamp() != null ? dto.getUploadTimestamp() : 0L)
                    .setLocation(dto.getLocation() != null ? dto.getLocation() : "")
                    .setLatitude(dto.getLatitude() != null ? dto.getLatitude() : 0.0)
                    .setLongitude(dto.getLongitude() != null ? dto.getLongitude() : 0.0);

            // Dodaj tags
            if (dto.getTags() != null) {
                builder.addAllTags(dto.getTags());
            }

            // Dodaj transcode profiles
            if (dto.getTranscodeProfiles() != null) {
                builder.addAllTranscodeProfiles(dto.getTranscodeProfiles());
            }

            UploadEventProto.UploadEvent event = builder.build();
            return event.toByteArray();

        } catch (Exception e) {
            logger.error("Greška pri serijalizaciji u Protobuf", e);
            throw new RuntimeException("Protobuf serialization failed", e);
        }
    }

    /**
     * Deserijalizuje Protobuf byte array u UploadEvent
     */
    public UploadEventDTO deserializeFromProtobuf(byte[] data) {
        try {
            UploadEventProto.UploadEvent event = UploadEventProto.UploadEvent.parseFrom(data);

            return UploadEventDTO.builder()
                    .videoId(event.getVideoId())
                    .title(event.getTitle())
                    .description(event.getDescription())
                    .tags(event.getTagsList())
                    .authorEmail(event.getAuthorEmail())
                    .authorName(event.getAuthorName())
                    .videoSizeBytes(event.getVideoSizeBytes())
                    .thumbnailSizeBytes(event.getThumbnailSizeBytes())
                    .videoPath(event.getVideoPath())
                    .thumbnailPath(event.getThumbnailPath())
                    .uploadTimestamp(event.getUploadTimestamp())
                    .location(event.getLocation())
                    .latitude(event.getLatitude())
                    .longitude(event.getLongitude())
                    .transcodeProfiles(event.getTranscodeProfilesList())
                    .build();

        } catch (Exception e) {
            logger.error("Greška pri deserijalizaciji Protobuf podataka", e);
            throw new RuntimeException("Protobuf deserialization failed", e);
        }
    }
}

