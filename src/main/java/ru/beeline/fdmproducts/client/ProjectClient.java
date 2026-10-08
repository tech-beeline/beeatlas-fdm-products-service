/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.beeline.fdmproducts.dto.project.UseCaseOperationsGroupedDTO;
import ru.beeline.fdmproducts.exception.ExternalServiceException;

import java.util.List;

@Slf4j
@Component
public class ProjectClient {

    private static final String PROJECT_UNAVAILABLE = "Сервис project недоступен";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ProjectClient(RestTemplate restTemplate,
                         ObjectMapper objectMapper,
                         @Value("${integration.project-server-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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

    public List<UseCaseOperationsGroupedDTO> findOperationsGroupedByUseCase(List<Integer> operationIds) {
        String url = baseUrl + "/api/v1/project/use-cases/operations/grouped-by-use-case";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        log.info("POST {} — методов: {}", url, operationIds.size());
        try {
            List<UseCaseOperationsGroupedDTO> result = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(operationIds, headers),
                    new ParameterizedTypeReference<List<UseCaseOperationsGroupedDTO>>() {
                    }).getBody();
            return result == null ? List.of() : result;
        } catch (RestClientResponseException e) {
            log.error("Ошибка вызова {}: статус {}, тело {}", url, e.getRawStatusCode(), e.getResponseBodyAsString());
            HttpStatus status = HttpStatus.resolve(e.getRawStatusCode());
            if (status != null && status.is4xxClientError()) {
                String message = extractErrorMessage(e.getResponseBodyAsString());
                throw new ExternalServiceException(status,
                        message != null && !message.isBlank() ? message : e.getStatusText());
            }
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, PROJECT_UNAVAILABLE);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка вызова {}: {}", url, e.getMessage(), e);
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, PROJECT_UNAVAILABLE);
        }
    }

    private String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode errorMessage = node.get("errorMessage");
            if (errorMessage != null && !errorMessage.isNull()) {
                return errorMessage.asText();
            }
        } catch (Exception e) {
            log.debug("Не удалось разобрать errorMessage из ответа project: {}", e.getMessage());
        }
        return body;
    }
}
