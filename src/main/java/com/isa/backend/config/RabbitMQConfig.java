package com.isa.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String VIDEO_UPLOAD_EXCHANGE = "video.upload.exchange";
    public static final String VIDEO_UPLOAD_QUEUE_JSON = "video.upload.queue.json";
    public static final String VIDEO_UPLOAD_QUEUE_PROTOBUF = "video.upload.queue.protobuf";
    public static final String VIDEO_UPLOAD_ROUTING_KEY_JSON = "video.upload.json";
    public static final String VIDEO_UPLOAD_ROUTING_KEY_PROTOBUF = "video.upload.protobuf";

    /**
     * Topic Exchange - omogućava routing na osnovu routing key-a
     */
    @Bean
    public TopicExchange videoUploadExchange() {
        return new TopicExchange(VIDEO_UPLOAD_EXCHANGE);
    }

    /**
     * Queue za JSON poruke
     */
    @Bean
    public Queue videoUploadQueueJson() {
        return new Queue(VIDEO_UPLOAD_QUEUE_JSON, true); // durable queue
    }

    /**
     * Queue za Protobuf poruke
     */
    @Bean
    public Queue videoUploadQueueProtobuf() {
        return new Queue(VIDEO_UPLOAD_QUEUE_PROTOBUF, true); // durable queue
    }

    /**
     * Binding za JSON queue
     */
    @Bean
    public Binding bindingJson(Queue videoUploadQueueJson, TopicExchange videoUploadExchange) {
        return BindingBuilder
                .bind(videoUploadQueueJson)
                .to(videoUploadExchange)
                .with(VIDEO_UPLOAD_ROUTING_KEY_JSON);
    }

    /**
     * Binding za Protobuf queue
     */
    @Bean
    public Binding bindingProtobuf(Queue videoUploadQueueProtobuf, TopicExchange videoUploadExchange) {
        return BindingBuilder
                .bind(videoUploadQueueProtobuf)
                .to(videoUploadExchange)
                .with(VIDEO_UPLOAD_ROUTING_KEY_PROTOBUF);
    }

    /**
     * Jackson converter za JSON serijalizaciju
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate sa JSON converterom (default)
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

