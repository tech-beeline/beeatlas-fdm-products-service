package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SlaV2DTO {

    private Integer rps;
    private Integer latency;
    private Double errorRate;
}