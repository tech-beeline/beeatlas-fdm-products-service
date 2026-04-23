/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmproducts.dto.dashboard.E2eProcessInfoDTO;
import ru.beeline.fdmproducts.dto.dashboard.GetInfoProcessDTO;

import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class DashboardClient {

    RestTemplate restTemplate;
    private final String dashboardServerUrl;

    public DashboardClient(@Value("${integration.dashboard-scenarios-server-url}") String dashboardServerUrl,
                           RestTemplate restTemplate) {
        this.dashboardServerUrl = dashboardServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<E2eProcessInfoDTO> getE2eSystemInfo(String cmdb) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headers);
            String url = "/api/v4/systems/" + cmdb + "/e2e";
            log.info("Request url: " + url);
            List<E2eProcessInfoDTO> result = restTemplate.exchange(dashboardServerUrl + url,
                    HttpMethod.GET, entity, new ParameterizedTypeReference<List<E2eProcessInfoDTO>>() {
                    }).getBody();
            log.info("Response size: " + (result != null ? result.size() : "null"));
            return result;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error("Информация по " + cmdb + " не найдена");
                return null;
            }
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка вызова к серверу Dashboard " + e.getMessage(), e);
        }
        return null;
    }

    public List<GetInfoProcessDTO> getInfoMessage(String uid) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headers);

            String url = dashboardServerUrl + "/api/v4/e2e/scenarios/{uid}/messages";
            log.info("Calling URL with scenarioId: " + uid);
            List<GetInfoProcessDTO> request = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<GetInfoProcessDTO>>() {
                    },
                    uid
            ).getBody();
            log.info("Response size: " + (request != null ? request.size() : "null"));
            return request;
        } catch (Exception e) {
            log.error("Ошибка вызова к серверу Dashboard для UID: " + uid, e);
        }
        return null;
    }
}
