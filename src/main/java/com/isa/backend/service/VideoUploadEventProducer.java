package com.isa.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isa.backend.config.RabbitMQConfig;
import com.isa.backend.dto.UploadEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Producer servis - šalje UploadEvent poruke u RabbitMQ
 * Podržava slanje u JSON i Protobuf formatu
 */
@Service
public class VideoUploadEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(VideoUploadEventProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Šalje poruku u JSON formatu
     */
    public void sendJsonEvent(UploadEventDTO event) {
        try {
            logger.info("Šaljem JSON poruku za video: {}", event.getVideoId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.VIDEO_UPLOAD_EXCHANGE,
                    RabbitMQConfig.VIDEO_UPLOAD_ROUTING_KEY_JSON,
                    event
            );
            logger.info("JSON poruka uspešno poslata za video: {}", event.getVideoId());
        } catch (Exception e) {
            logger.error("Greška pri slanju JSON poruke", e);
            throw new RuntimeException("Failed to send JSON event", e);
        }
    }

    /**
     * Šalje poruku u Protobuf formatu (dummy byte array za sada)
     */
    public void sendProtobufEvent(byte[] protobufBytes) {
        try {
            logger.info("Šaljem Protobuf poruku ({} bytes)", protobufBytes.length);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.VIDEO_UPLOAD_EXCHANGE,
                    RabbitMQConfig.VIDEO_UPLOAD_ROUTING_KEY_PROTOBUF,
                    protobufBytes
            );
            logger.info("Protobuf poruka uspešno poslata");
        } catch (Exception e) {
            logger.error("Greška pri slanju Protobuf poruke", e);
            throw new RuntimeException("Failed to send Protobuf event", e);
        }
    }
}

