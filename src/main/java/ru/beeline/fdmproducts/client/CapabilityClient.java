//package ru.beeline.fdmproducts.client;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import ru.beeline.fdmproducts.dto.SearchCapabilityDTO;
//
//import java.util.List;
//
//@Slf4j
//@Service
//public class CapabilityClient {
//
//    RestTemplate restTemplate;
//
//    private final String capabilityServerUrl;
//
//    public CapabilityClient(@Value("${integration.capability-server-url}") String capabilityServerUrl,
//                            RestTemplate restTemplate) {
//        this.capabilityServerUrl = capabilityServerUrl;
//        this.restTemplate = restTemplate;
//    }
//
//    public SearchCapabilityDTO getCapabilities(String code) {
//        try {
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.add("SOURCE", "Sparx");
//            HttpEntity<String> entity = new HttpEntity<>(headers);
//
//            SearchCapabilityDTO result = restTemplate.exchange(capabilityServerUrl + "/api/v1/find?search="+code,
//                    HttpMethod.GET, entity, new ParameterizedTypeReference<SearchCapabilityDTO>() {
//            }).getBody();
//
//            return result;
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
//        return null;
//    }
//}
