package ru.beeline.fdmproducts.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.service.ArchContainerRelationsService;


@Slf4j
@Component
@EnableRabbit
public class DeleteArchRelationsConsumer {

    @Autowired
    ArchContainerRelationsService archContainerRelationsService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "${queue.delete-arch-container-relations.name}")
    public void containerQueue(String message) {
        log.info("Received message from delete-arch-container-relations: " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("entityId") && jsonNode.has("changeType")) {
                if (jsonNode.get("changeType").asText().equals("DELETE")) {
                    archContainerRelationsService.processContainerDelete(jsonNode.get("entityId").asInt());
                }
            } else {
                log.error("Message does not match the required format");
            }

        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

    @RabbitListener(queues = "${queue.delete-arch-interface-relations.name}")
    public void interfaceQueue(String message) {
        log.info("Received message from delete-arch-interface-relations: " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("entityId") && jsonNode.has("changeType")) {
                if (jsonNode.get("changeType").asText().equals("DELETE")) {
                    archContainerRelationsService.processInterfaceDelete(jsonNode.get("entityId").asInt());
                }
            } else {
                log.error("Message does not match the required format");
            }

        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

    @RabbitListener(queues = "${queue.delete-arch-operation-relations.name}")
    public void operationQueue(String message) {
        log.info("Received message from delete-arch-operation-relations: " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("entityId") && jsonNode.has("changeType")) {
                if (jsonNode.get("changeType").asText().equals("DELETE")) {
                    archContainerRelationsService.processOperationDelete(jsonNode.get("entityId").asInt());
                }
            } else {
                log.error("Message does not match the required format");
            }

        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

}