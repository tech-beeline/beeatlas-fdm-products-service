package ru.beeline.fdmproducts.dto.dashboard;


import lombok.*;

/**
 * Request body for the PUT /api/v1/source-metric endpoint.
 * The field is mandatory – it must be non‑null and not empty.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SourceMetricRequestDto {

    private String sourceMetric;

}