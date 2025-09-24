package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SlaV2DTO {

    private Double rps;
    private Double latency;
    private Double errorRate;
}