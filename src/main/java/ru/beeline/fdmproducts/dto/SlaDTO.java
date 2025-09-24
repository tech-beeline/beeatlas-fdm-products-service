package ru.beeline.fdmproducts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SlaDTO {

    private Double rps;
    private Double latency;
    @JsonProperty("error_rate")
    private Double errorRate;
}
