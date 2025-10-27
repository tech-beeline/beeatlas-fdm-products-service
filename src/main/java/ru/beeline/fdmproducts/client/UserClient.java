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
import ru.beeline.fdmlib.dto.auth.UserInfoDTO;
import ru.beeline.fdmlib.dto.auth.UserProfileDTO;
import ru.beeline.fdmlib.dto.auth.UserProfileShortDTO;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UserClient {
    RestTemplate restTemplate;
    private final String userServerUrl;

    public UserClient(@Value("${integration.auth-server-url}") String userServerUrl, RestTemplate restTemplate) {
        this.userServerUrl = userServerUrl;
        this.restTemplate = restTemplate;
    }

    public UserInfoDTO getUserInfo(String email, String fullName, String idExt) {
        String login = email.substring(0, email.indexOf("@"));
        UserInfoDTO userInfoDto = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            userInfoDto = restTemplate.exchange(userServerUrl + "/api/admin/v1/user/" + login + "/info?&email=" + email + "&fullName=" + fullName + "&idExt=" + idExt,
                    HttpMethod.GET,
                    entity,
                    UserInfoDTO.class).getBody();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return userInfoDto;
    }

    public List<UserProfileShortDTO> findUserProfiles(List<Integer> userIds) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Integer>> entity = new HttpEntity<>(userIds, headers);

            ResponseEntity<List<UserProfileShortDTO>> response = restTemplate.exchange(
                    userServerUrl + "/api/v1/user/list",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching user profiles: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public UserProfileDTO findUserProfilesById(Integer id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<Integer>> entity = new HttpEntity<>(headers);
            ResponseEntity<UserProfileDTO> response = restTemplate.exchange(
                    userServerUrl + "/api/v1/user/" + id,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (Exception e) {
            log.error("Error fetching user profiles: {}", e.getMessage());
            return null;
        }
    }
}
