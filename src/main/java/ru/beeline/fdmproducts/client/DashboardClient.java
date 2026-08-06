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
import ru.beeline.fdmproducts.dto.dashboard.E2eMethodUsagesDTO;
import ru.beeline.fdmproducts.dto.dashboard.E2eProcessInfoDTO;

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

    public List<E2eMethodUsagesDTO> getE2eMethodUsages(String alias) {
        String url = dashboardServerUrl + "/api/v4/systems/" + alias + "/api-usage";
        log.info("Запрос api-usage в Dashboard: alias={}, url={}", alias, url);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(headers);
            List<E2eMethodUsagesDTO> result = restTemplate.exchange(url,
                    HttpMethod.GET, entity, new ParameterizedTypeReference<List<E2eMethodUsagesDTO>>() {
                    }).getBody();
            int methodsCount = result != null ? result.size() : 0;
            int usagesCount = result == null ? 0 : result.stream()
                    .filter(item -> item.getUsages() != null)
                    .mapToInt(item -> item.getUsages().size())
                    .sum();
            log.info("Ответ api-usage от Dashboard: alias={}, методов={}, использований={}",
                    alias, methodsCount, usagesCount);
            return result != null ? result : List.of();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.warn("Данные api-usage не найдены в Dashboard: alias={}", alias);
                return List.of();
            }
            log.error("Ошибка api-usage в Dashboard: alias={}, статус={}, сообщение={}",
                    alias, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Сбой вызова api-usage в Dashboard: alias={}",
                    alias, e);
        }
        return List.of();
    }
}
