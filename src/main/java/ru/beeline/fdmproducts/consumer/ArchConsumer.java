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
public class ArchConsumer {

    @Autowired
    ArchContainerRelationsService archContainerRelationsService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "${queue.delete-arch-container-relations.name}")
    public void containerQueue(String message) {
        log.info("Received message from delete-arch-container-relations: " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("id") && jsonNode.has("changeType")) {
                if (jsonNode.get("changeType").asText().equals("DELETE")) {
                    archContainerRelationsService.processContainerDelete(jsonNode.get("id").asInt());
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
            if (jsonNode.has("id") && jsonNode.has("changeType")) {
                if (jsonNode.get("changeType").asText().equals("DELETE")) {
                    archContainerRelationsService.processInterfaceDelete(jsonNode.get("id").asInt());
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
            if (jsonNode.has("id") && jsonNode.has("changeType")) {
                if (jsonNode.get("changeType").asText().equals("DELETE")) {
                    archContainerRelationsService.processOperationDelete(jsonNode.get("id").asInt());
                }
            } else {
                log.error("Message does not match the required format");
            }

        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

    @RabbitListener(queues = "${queue.comparison-arch-operations.name}")
    public void comparisonArchOperations(String message) {
        log.info("Received message from comparison-arch-operations: " + message, new String(message.getBytes()));
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("id") && jsonNode.has("changeType")) {
                String name = jsonNode.get("name") == null ? "" : jsonNode.get("name").asText();
                String type = jsonNode.get("changeType").asText();
                if (type.equals("CREATE") || type.equals("UPDATE")) {
                    archContainerRelationsService.processOperationComparison(jsonNode.get("id").asInt(),
                                                                             name,
                                                                             type);
                }
            } else {
                log.error("Message does not match the required format");
            }

        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

}