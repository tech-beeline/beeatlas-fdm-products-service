/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmproducts.dto.ProductInfluenceDTO;


@Slf4j
@Service
public class GraphClient {

    RestTemplate restTemplate;
    private final String graphServerUrl;

    public GraphClient(@Value("${integration.graph-server-url}") String graphServerUrl, RestTemplate restTemplate) {
        this.graphServerUrl = graphServerUrl;
        this.restTemplate = restTemplate;
    }

    public ProductInfluenceDTO getInfluences(String cmdb) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<ProductInfluenceDTO> response = restTemplate.exchange(graphServerUrl + "/api/v1/graph/product/" + cmdb + "/influence",
                                                                                 HttpMethod.GET,
                                                                                 requestEntity,
                                                                                 ProductInfluenceDTO.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Product not found (404) for cmdb: " + cmdb);
            return null;
        } catch (Exception e) {
            log.error("Error while posting product to CMDB: " + e.getMessage(), e);
            throw new RuntimeException("Error during post request", e);
        }
    }

}
