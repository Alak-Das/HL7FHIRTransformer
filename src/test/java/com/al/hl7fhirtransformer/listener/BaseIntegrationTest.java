package com.al.hl7fhirtransformer.listener;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.GenericContainer;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class BaseIntegrationTest {

    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @SuppressWarnings("resource")
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @BeforeAll
    static void startContainers() {
        mongoDBContainer.start();
        rabbitMQContainer.start();
        redisContainer.start();
        
        // Force system properties to override application.properties defaults
        System.setProperty("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl());
        System.setProperty("spring.rabbitmq.host", rabbitMQContainer.getHost());
        System.setProperty("spring.rabbitmq.port", String.valueOf(rabbitMQContainer.getAmqpPort()));
        System.setProperty("spring.rabbitmq.username", "guest");
        System.setProperty("spring.rabbitmq.password", "guest");
        System.setProperty("spring.data.redis.host", redisContainer.getHost());
        System.setProperty("spring.data.redis.port", String.valueOf(redisContainer.getMappedPort(6379)));
    }
}
