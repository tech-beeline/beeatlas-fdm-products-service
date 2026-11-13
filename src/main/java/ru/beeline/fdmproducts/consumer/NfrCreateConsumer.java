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
import ru.beeline.fdmproducts.client.BpmClient;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableRabbit
public class NfrCreateConsumer {

    @Autowired
    private NonFunctionalRequirementEnumRepository enumRepository;
    @Autowired
    private BpmClient bpmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "${queue.nfr-create.name}")
    public void onNfrCreate(String message) {
        log.info("Received message from nfr_create: {}", message);
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (!jsonNode.hasNonNull("id") || !jsonNode.hasNonNull("changeType")) {
                log.error("nfr_create: сообщение без id или changeType");
                return;
            }
            String changeType = jsonNode.get("changeType").asText();
            if (!"CREATE".equals(changeType)) {
                log.info("nfr_create: changeType {} пропущен", changeType);
                return;
            }
            int id = jsonNode.get("id").asInt();
            NonFunctionalRequirementEnum nfr = enumRepository.findById(id).orElse(null);
            if (nfr == null) {
                log.error("nfr_create: версия НФТ {} не найдена", id);
                return;
            }
            bpmClient.startAutoNfrProcess(nfr.getId(), parseRuleCodesLower(nfr.getRule()));
        } catch (Exception e) {
            log.error("nfr_create: ошибка обработки сообщения: {}", e.getMessage(), e);
        }
    }

    static List<String> parseRuleCodesLower(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return List.of();
        }
        String ruleWithoutSpaces = rule.replaceAll("\\s+", "");
        return Arrays.stream(ruleWithoutSpaces.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> code.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }
}
