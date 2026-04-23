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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmproducts.dto.PatternDTO;
import ru.beeline.fdmproducts.dto.ProductAvailableDTO;
import ru.beeline.fdmproducts.dto.techradar.TechAdvancedGetDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrPatternDTO;
import ru.beeline.fdmproducts.exception.DatabaseConnectionException;

import java.util.Collections;
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
            List<ru.beeline.fdmproducts.dto.PatternDTO> result = restTemplate.exchange(techradarServerUrl + "/api/v1/patterns/auto-check",
                    HttpMethod.GET, entity, new ParameterizedTypeReference<List<ru.beeline.fdmproducts.dto.PatternDTO>>() {
                    }).getBody();
            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public List<TechAdvancedGetDTO> getTechById(List<Integer> ids) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = techradarServerUrl + "/api/v1/tech/by-ids?ids=" +
                    ids.stream()
                            .map(String::valueOf)
                            .reduce((a, b) -> a + "," + b)
                            .orElse("");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            List<TechAdvancedGetDTO> result = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<TechAdvancedGetDTO>>() {
                    }
            ).getBody();

            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error calling getTechById: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public ProductAvailableDTO checkPatternsAvailability(List<Integer> patterns) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<Integer>> entity = new HttpEntity<>(patterns, headers);
            ResponseEntity<ProductAvailableDTO> response = restTemplate.exchange(
                    techradarServerUrl + "/api/v1/pattern/availability",
                    HttpMethod.POST,
                    entity,
                    ProductAvailableDTO.class
            );
            return response.getBody();
        } catch (ResourceAccessException e) {
            log.error("Сервис {} недоступен. Проверьте подключение.", techradarServerUrl);
            throw new DatabaseConnectionException("Сервис " + techradarServerUrl + " недоступен. Проверьте подключение.");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public List<NfrPatternDTO> getPatternsByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = techradarServerUrl + "/api/v1/pattern/by-ids?ids=" +
                    ids.stream()
                            .map(String::valueOf)
                            .reduce((a, b) -> a + "," + b)
                            .orElse("");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            List<NfrPatternDTO> result = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<NfrPatternDTO>>() {
                    }
            ).getBody();
            return result != null ? result : Collections.emptyList();
        } catch (ResourceAccessException e) {
            log.error("Сервис {} недоступен. Проверьте подключение.", techradarServerUrl);
            throw new DatabaseConnectionException("Сервис " + techradarServerUrl + " недоступен. Проверьте подключение.");
        } catch (Exception e) {
            log.error("Error calling getPatternsByIds: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
