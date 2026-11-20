package ru.beeline.fdmproducts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Источник метрик обнаруженного интерфейса.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiscoveredInterfaceMetricDto {
    private Integer id;
    private String externalId;
    private String name;
    private String source;
    private String productAlias;
    private String sourceMetric;
}
