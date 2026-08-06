/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BpmClient {

    private final String bpmServerUrl;
    private final RestTemplate restTemplate;

    public BpmClient(@Value("${integration.bpm-server-url}") String bpmServerUrl,
                     RestTemplate restTemplate) {
        this.bpmServerUrl = bpmServerUrl;
        this.restTemplate = restTemplate;
    }

    public void startNfrAutoAssign(Integer nfrId, List<String> rule) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of("nfrId", nfrId, "rule", rule != null ? rule : List.of());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.exchange(bpmServerUrl + "/api/v1/nfr/auto-assign",
                    HttpMethod.POST, entity, Void.class);
            log.info("BPM auto-nfr-all process started for nfrId={}", nfrId);
        } catch (Exception e) {
            log.error("Failed to start BPM auto-nfr-all process for nfrId={}: {}", nfrId, e.getMessage(), e);
        }
    }
}
