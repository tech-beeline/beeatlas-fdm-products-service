package ru.beeline.fdmproducts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class SourceMetricDto {
    private List<ProductMetricDto> products;
    private List<ContainerMetricDto> containers;
    private List<InterfaceMetricDto> interfaces;
}