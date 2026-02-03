/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmproducts.dto.PatternDTO;

import java.util.List;


@Slf4j
@Service
public class TechradarClient {
    RestTemplate restTemplate;
    private final String techradarServerUrl;

    public TechradarClient(@Value("${integration.techradar-server-url}") String techradarServerUrl,
                           RestTemplate restTemplate) {
        this.techradarServerUrl = techradarServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<PatternDTO> getPatternsAutoCheck() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<PatternDTO> result = restTemplate.exchange(techradarServerUrl + "/api/v1/patterns/auto-check",
                    HttpMethod.GET, entity, new ParameterizedTypeReference<List<PatternDTO>>() {
                    }).getBody();
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
