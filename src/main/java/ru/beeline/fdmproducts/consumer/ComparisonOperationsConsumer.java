/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.service.ComparisonOperationsService;


@Slf4j
@Component
@EnableRabbit
public class ComparisonOperationsConsumer {

    @Autowired
    ComparisonOperationsService comparisonOperationsService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "${queue.comparison-operations.name}")
    public void techQueue(String message) {
        log.info("Received message from comparison_operations: " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("id") && jsonNode.has("changeType")) {
                comparisonOperationsService.process(jsonNode.get("id").asInt());
            } else {
                log.error("Message does not match the required format");
            }

        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

}