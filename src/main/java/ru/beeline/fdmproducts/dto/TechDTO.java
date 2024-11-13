package ru.beeline.fdmproducts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class TechDTO {

    @JsonProperty("tech_id")
    private Integer techId;
    private String label;
}
