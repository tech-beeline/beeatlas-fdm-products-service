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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmproducts.dto.project.OperationUseCasesResponseDTO;
import ru.beeline.fdmproducts.exception.ExternalServiceException;

import java.util.List;

@Slf4j
@Component
public class ProjectClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProjectClient(RestTemplate restTemplate,
                         @Value("${integration.project-server-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public List<OperationUseCasesResponseDTO> findUseCasesByOperations(List<Integer> operationIds) {
        String url = baseUrl + "/api/v1/project/use-cases/by-operations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        log.info("POST {} — методов: {}", url, operationIds.size());
        try {
            List<OperationUseCasesResponseDTO> result = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(operationIds, headers),
                    new ParameterizedTypeReference<List<OperationUseCasesResponseDTO>>() {
                    }).getBody();
            return result == null ? List.of() : result;
        } catch (RestClientResponseException e) {
            log.error("Ошибка вызова {}: статус {}, тело {}", url, e.getRawStatusCode(), e.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(e.getRawStatusCode());
            throw new ExternalServiceException(
                    status == null || status.is5xxServerError() ? HttpStatus.BAD_GATEWAY : status,
                    "Ошибка сервиса project при поиске use case по методам: " + e.getResponseBodyAsString());
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка вызова {}: {}", url, e.getMessage(), e);
            throw new ExternalServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Сервис project недоступен: " + e.getMessage());
        }
    }
}
