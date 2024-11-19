package ru.beeline.fdmproducts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ApiSecretDTO {

    private Integer id;
    @JsonProperty("api_secret")
    private String apiSecret;
}
