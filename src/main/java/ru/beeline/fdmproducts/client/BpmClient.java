/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BpmClient {

    private final String bpmServerUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BpmClient(@Value("${integration.bpm-server-url}") String bpmServerUrl,
                     RestTemplate restTemplate,
                     ObjectMapper objectMapper) {
        this.bpmServerUrl = bpmServerUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void startAutoNfrProcess(Integer nfrId, List<String> rule) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("businessKey", String.valueOf(nfrId));
        body.put("isSync", false);
        body.put("variables", Map.of(
                "nfrId", Map.of("value", nfrId, "type", "Integer"),
                "rule", ruleVariable(rule != null ? rule : List.of())
        ));
        String url = bpmServerUrl.replaceAll("/$", "")
                + "/engine-rest/process-definition/key/Process_1t2rjem/start";
        restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
        log.info("Процесс Process_1t2rjem запущен для nfrId={}", nfrId);
    }

    private Map<String, Object> ruleVariable(List<String> rule) {
        try {
            return Map.of(
                    "value", objectMapper.writeValueAsString(rule),
                    "type", "Object",
                    "valueInfo", Map.of(
                            "objectTypeName", "java.util.ArrayList",
                            "serializationDataFormat", "application/json"
                    )
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать rule", e);
        }
    }
}
