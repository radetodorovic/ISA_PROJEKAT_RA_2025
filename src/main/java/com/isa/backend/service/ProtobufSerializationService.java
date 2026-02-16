package com.isa.backend.service;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;
import com.isa.backend.dto.UploadEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Servis za konverziju izmedju DTO i Protobuf formata
 */
@Service
public class ProtobufSerializationService {

    private static final Logger logger = LoggerFactory.getLogger(ProtobufSerializationService.class);

    /**
     * Konvertuje UploadEventDTO u Protobuf format i serijalizuje
     */
    public byte[] serializeToProtobuf(UploadEventDTO dto) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CodedOutputStream cos = CodedOutputStream.newInstance(out);

            writeString(cos, 1, dto.getVideoId());
            writeString(cos, 2, dto.getTitle());
            writeString(cos, 3, dto.getDescription());
            writeStringList(cos, 4, dto.getTags());
            writeString(cos, 5, dto.getAuthorEmail());
            writeString(cos, 6, dto.getAuthorName());
            writeInt64(cos, 7, dto.getVideoSizeBytes());
            writeInt64(cos, 8, dto.getThumbnailSizeBytes());
            writeString(cos, 9, dto.getVideoPath());
            writeString(cos, 10, dto.getThumbnailPath());
            writeInt64(cos, 11, dto.getUploadTimestamp());
            writeString(cos, 12, dto.getLocation());
            writeDouble(cos, 13, dto.getLatitude());
            writeDouble(cos, 14, dto.getLongitude());
            writeStringList(cos, 15, dto.getTranscodeProfiles());

            cos.flush();
            return out.toByteArray();

        } catch (Exception e) {
            logger.error("Greska pri serijalizaciji u Protobuf", e);
            throw new RuntimeException("Protobuf serialization failed", e);
        }
    }

    /**
     * Deserijalizuje Protobuf byte array u UploadEvent
     */
    public UploadEventDTO deserializeFromProtobuf(byte[] data) {
        try {
            CodedInputStream cis = CodedInputStream.newInstance(data);

            String videoId = "";
            String title = "";
            String description = "";
            List<String> tags = new ArrayList<>();
            String authorEmail = "";
            String authorName = "";
            long videoSizeBytes = 0L;
            long thumbnailSizeBytes = 0L;
            String videoPath = "";
            String thumbnailPath = "";
            long uploadTimestamp = 0L;
            String location = "";
            double latitude = 0.0;
            double longitude = 0.0;
            List<String> transcodeProfiles = new ArrayList<>();

            while (!cis.isAtEnd()) {
                int tag = cis.readTag();
                if (tag == 0) {
                    break;
                }

                int fieldNumber = WireFormat.getTagFieldNumber(tag);
                switch (fieldNumber) {
                    case 1 -> videoId = cis.readString();
                    case 2 -> title = cis.readString();
                    case 3 -> description = cis.readString();
                    case 4 -> tags.add(cis.readString());
                    case 5 -> authorEmail = cis.readString();
                    case 6 -> authorName = cis.readString();
                    case 7 -> videoSizeBytes = cis.readInt64();
                    case 8 -> thumbnailSizeBytes = cis.readInt64();
                    case 9 -> videoPath = cis.readString();
                    case 10 -> thumbnailPath = cis.readString();
                    case 11 -> uploadTimestamp = cis.readInt64();
                    case 12 -> location = cis.readString();
                    case 13 -> latitude = cis.readDouble();
                    case 14 -> longitude = cis.readDouble();
                    case 15 -> transcodeProfiles.add(cis.readString());
                    default -> {
                        if (!cis.skipField(tag)) {
                            break;
                        }
                    }
                }
            }

            return UploadEventDTO.builder()
                    .videoId(videoId)
                    .title(title)
                    .description(description)
                    .tags(tags)
                    .authorEmail(authorEmail)
                    .authorName(authorName)
                    .videoSizeBytes(videoSizeBytes)
                    .thumbnailSizeBytes(thumbnailSizeBytes)
                    .videoPath(videoPath)
                    .thumbnailPath(thumbnailPath)
                    .uploadTimestamp(uploadTimestamp)
                    .location(location)
                    .latitude(latitude)
                    .longitude(longitude)
                    .transcodeProfiles(transcodeProfiles)
                    .build();

        } catch (Exception e) {
            logger.error("Greska pri deserijalizaciji Protobuf podataka", e);
            throw new RuntimeException("Protobuf deserialization failed", e);
        }
    }

    private void writeString(CodedOutputStream cos, int field, String value) throws java.io.IOException {
        if (value != null && !value.isEmpty()) {
            cos.writeString(field, value);
        }
    }

    private void writeStringList(CodedOutputStream cos, int field, List<String> values) throws java.io.IOException {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                cos.writeString(field, value);
            }
        }
    }

    private void writeInt64(CodedOutputStream cos, int field, Long value) throws java.io.IOException {
        if (value != null && value != 0L) {
            cos.writeInt64(field, value);
        }
    }

    private void writeDouble(CodedOutputStream cos, int field, Double value) throws java.io.IOException {
        if (value != null && value != 0.0d) {
            cos.writeDouble(field, value);
        }
    }
}
