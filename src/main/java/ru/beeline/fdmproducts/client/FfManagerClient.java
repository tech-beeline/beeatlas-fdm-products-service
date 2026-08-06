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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmproducts.dto.ffmanager.FfManagerFitnessFunctionDTO;
import ru.beeline.fdmproducts.exception.DatabaseConnectionException;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class FfManagerClient {

    private final String ffManagerServerUrl;
    private final RestTemplate restTemplate;

    public FfManagerClient(@Value("${integration.ff-manager-url}") String ffManagerServerUrl,
                           RestTemplate restTemplate) {
        this.ffManagerServerUrl = ffManagerServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<FfManagerFitnessFunctionDTO> getAllFitnessFunctions() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.info("Запрос в ff-manager: {}/api/v1/fitness-functions", ffManagerServerUrl);
            List<FfManagerFitnessFunctionDTO> result = restTemplate.exchange(
                    ffManagerServerUrl + "/api/v1/fitness-functions",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<FfManagerFitnessFunctionDTO>>() {
                    }
            ).getBody();
            return result != null ? result : Collections.emptyList();
        } catch (ResourceAccessException e) {
            log.error("Сервис {} недоступен.", ffManagerServerUrl);
            throw new DatabaseConnectionException("Сервис " + ffManagerServerUrl + " недоступен. Проверьте подключение.");
        } catch (Exception e) {
            log.error("Error calling getAllFitnessFunctions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
