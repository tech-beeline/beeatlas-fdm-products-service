package ru.beeline.fdmproducts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SlaDTO {

    private Integer rps;
    private Integer latency;
    @JsonProperty("error_rate")
    private Double errorRate;
}
