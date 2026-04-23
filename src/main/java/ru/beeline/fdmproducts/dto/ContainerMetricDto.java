package ru.beeline.fdmproducts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Описание метрики на уровне контейнера.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContainerMetricDto {
    private Integer id;
    private String code;
    private String sourceMetric;
}