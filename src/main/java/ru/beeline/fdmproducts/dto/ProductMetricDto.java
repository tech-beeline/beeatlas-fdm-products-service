package ru.beeline.fdmproducts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Описание метрики на уровне продукта.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductMetricDto {
    private Integer id;
    private String alias;
    private String sourceMetric;
}