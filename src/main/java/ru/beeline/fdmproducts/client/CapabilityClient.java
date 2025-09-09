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
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmproducts.dto.IdCodeDTO;
import ru.beeline.fdmproducts.dto.SearchCapabilityDTO;
import ru.beeline.fdmproducts.dto.TcDTO;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CapabilityClient {

    RestTemplate restTemplate;

    private final String capabilityServerUrl;

    public CapabilityClient(@Value("${integration.capability-server-url}") String capabilityServerUrl, RestTemplate restTemplate) {
        this.capabilityServerUrl = capabilityServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<SearchCapabilityDTO> getCapabilities(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("SOURCE", "Sparx");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            List<SearchCapabilityDTO> result = restTemplate.exchange(capabilityServerUrl +
                            "/api/v1/find?search=" + code + "&findBy=TECH_CAPABILITY",
                    HttpMethod.GET, entity, new ParameterizedTypeReference<List<SearchCapabilityDTO>>() {
                    }).getBody();

            return result;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public List<TcDTO> getTcs(List<Integer> tcIds) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String tcIdsList = tcIds.stream()
                    .filter(Objects::nonNull)
                    .map(id -> "ids=" + id)
                    .collect(Collectors.joining("&"));
            String fullUrl = capabilityServerUrl + "/api/v1/tech-capabilities/list/by-ids?" + tcIdsList;
            log.debug("Запрос к сервису: {}", fullUrl);

            ResponseEntity<List<TcDTO>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<TcDTO>>() {
                    }
            );
            log.debug("Статус ответа: {}", response.getStatusCode());
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Ошибка при получении TcDTO: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<IdCodeDTO> getIdCodes(List<String> codes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String tcCodeList = codes.stream()
                    .filter(Objects::nonNull)
                    .map(code -> "codes=" + code)
                    .collect(Collectors.joining("&"));
            String fullUrl = capabilityServerUrl + "/api/v1/tech-capabilities/by-code?" + tcCodeList;
            log.debug("Запрос к сервису: {}", fullUrl);

            ResponseEntity<List<IdCodeDTO>> response = restTemplate.exchange(
                    fullUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {
                    });
            log.debug("Статус ответа: {}", response.getStatusCode());
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Ошибка при получении TcDTO: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
