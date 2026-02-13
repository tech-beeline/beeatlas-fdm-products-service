package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.dto.ContainerMetricDto;
import ru.beeline.fdmproducts.dto.InterfaceMetricDto;
import ru.beeline.fdmproducts.dto.ProductMetricDto;
import ru.beeline.fdmproducts.dto.SourceMetricDto;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class SourceMetricService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private InterfaceRepository interfaceRepository;

    public SourceMetricDto getAllMetrics() {
        return SourceMetricDto.builder()
                .products(productRepository.findAllBySourceMetricIsNotNull()
                                  .stream()
                                  .map(product->ProductMetricDto.builder()
                                          .alias(product.getAlias())
                                          .id(product.getId())
                                          .sourceMetric(product.getSourceMetric()).build())
                                  .collect(Collectors.toUnmodifiableList()))
                .containers(containerRepository.findAllBySourceMetricIsNotNullAndDeletedDateIsNull()
                                    .stream()
                                    .map(containerProduct-> ContainerMetricDto.builder()
                                            .code(containerProduct.getCode())
                                            .id(containerProduct.getId())
                                            .sourceMetric(containerProduct.getSourceMetric()).build())
                                    .collect(Collectors.toUnmodifiableList()))
                .interfaces(interfaceRepository.findAllBySourceMetricIsNotNullAndDeletedDateIsNull()
                                    .stream()
                                    .map(anInterface-> InterfaceMetricDto.builder()
                                            .code(anInterface.getCode())
                                            .id(anInterface.getId())
                                            .sourceMetric(anInterface.getSourceMetric()).build())
                                    .collect(Collectors.toUnmodifiableList()))
                .build();
    }
}