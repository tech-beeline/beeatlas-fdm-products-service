package ru.beeline.fdmproducts.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.service.ProductService;


@Slf4j
@Component
@EnableRabbit
public class ProductConsumer {

    @Autowired
    ProductService productService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "${queue.update-product-owner-and-priority-by-cmdb.name}")
    public void updateOwnerAndPriority(String message) {
        log.info("Received message from update-product-owner-and-priority-by-cmdb: " + message,
                 new String(message.getBytes()));
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("cmdb") && jsonNode.has("owner") && jsonNode.has("critical")) {
                JsonNode ownerNode = jsonNode.get("owner");
                if (ownerNode != null && ownerNode.has("fullName") && ownerNode.has("email") && ownerNode.has("extId") && ownerNode.has(
                        "login")) {
                    productService.updateOwnerAndPriority(jsonNode.get("cmdb").asText(),
                                                          ownerNode.get("email").asText(),
                                                          ownerNode.get("fullName").asText(),
                                                          jsonNode.get("critical").asText(),
                                                          ownerNode.get("extId").asText());
                } else {
                    log.error("Owner object does not contain required fields");
                }
            } else {
                log.error("Message does not match the required format");
            }
        } catch (Exception e) {
            log.error("Internal server Error: " + e.getMessage());
        }
    }

}