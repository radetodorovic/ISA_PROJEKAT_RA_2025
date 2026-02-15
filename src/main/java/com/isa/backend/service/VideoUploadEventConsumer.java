package com.isa.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isa.backend.config.RabbitMQConfig;
import com.isa.backend.dto.UploadEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Consumer servis - prima UploadEvent poruke iz RabbitMQ
 * Podržava primanje JSON i Protobuf formata
 */
@Service
public class VideoUploadEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoUploadEventConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageComparisonService comparisonService;

    @Autowired
    private ProtobufSerializationService protobufService;

    /**
     * Prima JSON poruke
     */
    @RabbitListener(queues = RabbitMQConfig.VIDEO_UPLOAD_QUEUE_JSON)
    public void receiveJsonEvent(UploadEventDTO event) {
        try {
            long startTime = System.nanoTime();

            // Ovde možeš dodati logiku za obradu eventa
            logger.info("Primljena JSON poruka za video: {} (naslov: {})",
                    event.getVideoId(), event.getTitle());
            logger.info("Autor: {}, Veličina videa: {} bytes",
                    event.getAuthorName(), event.getVideoSizeBytes());

            long endTime = System.nanoTime();
            long deserializationTime = endTime - startTime;

            // Zabeležimo vreme deserijalizacije
            comparisonService.recordJsonDeserialization(deserializationTime);

            logger.debug("JSON deserijalizacija trajala: {} ns", deserializationTime);

        } catch (Exception e) {
            logger.error("Greška pri obradi JSON poruke", e);
        }
    }

    /**
     * Prima Protobuf poruke
     */
    @RabbitListener(queues = RabbitMQConfig.VIDEO_UPLOAD_QUEUE_PROTOBUF)
    public void receiveProtobufEvent(byte[] message) {
        try {
            long startTime = System.nanoTime();

            // Protobuf deserijalizacija
            UploadEventDTO event = protobufService.deserializeFromProtobuf(message);

            logger.info("Primljena Protobuf poruka za video: {} (naslov: {})",
                    event.getVideoId(), event.getTitle());
            logger.info("Autor: {}, Veličina videa: {} bytes",
                    event.getAuthorName(), event.getVideoSizeBytes());

            long endTime = System.nanoTime();
            long deserializationTime = endTime - startTime;


            // Zabeležimo vreme deserijalizacije
            comparisonService.recordProtobufDeserialization(deserializationTime);

            logger.debug("Protobuf deserijalizacija trajala: {} ns", deserializationTime);

        } catch (Exception e) {
            logger.error("Greška pri obradi Protobuf poruke", e);
        }
    }
}

