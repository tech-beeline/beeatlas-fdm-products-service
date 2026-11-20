package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.ProductBranch;
import ru.beeline.fdmproducts.dto.ContainerMetricDto;
import ru.beeline.fdmproducts.dto.DiscoveredInterfaceMetricDto;
import ru.beeline.fdmproducts.dto.InterfaceMetricDto;
import ru.beeline.fdmproducts.dto.ProductMetricDto;
import ru.beeline.fdmproducts.dto.SourceMetricDto;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
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

    @Autowired
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;

    public SourceMetricDto getAllMetrics() {
        log.debug("SourceMetricService.getAllMetrics — выборка сущностей с непустым source_metric");
        SourceMetricDto result = SourceMetricDto.builder()
                .products(productRepository.findAllBySourceMetricIsNotNull()
                        .stream()
                        .map(product -> ProductMetricDto.builder()
                                .alias(product.getAlias())
                                .id(product.getId())
                                .sourceMetric(product.getSourceMetric()).build())
                        .collect(Collectors.toUnmodifiableList()))
                .containers(containerRepository.findAllBySourceMetricIsNotNullAndDeletedDateIsNull()
                        .stream()
                        .map(this::toContainerMetricDto)
                        .collect(Collectors.toUnmodifiableList()))
                .interfaces(interfaceRepository.findAllBySourceMetricIsNotNullAndDeletedDateIsNull()
                        .stream()
                        .map(this::toInterfaceMetricDto)
                        .collect(Collectors.toUnmodifiableList()))
                .discoveredInterfaces(discoveredInterfaceRepository
                        .findAllBySourceMetricIsNotNullAndDeletedDateIsNull()
                        .stream()
                        .map(this::toDiscoveredInterfaceMetricDto)
                        .collect(Collectors.toUnmodifiableList()))
                .build();
        log.debug("SourceMetricService.getAllMetrics — собрано products={}, containers={}, interfaces={}, discoveredInterfaces={}",
                result.getProducts().size(), result.getContainers().size(), result.getInterfaces().size(),
                result.getDiscoveredInterfaces().size());
        return result;
    }

    private ContainerMetricDto toContainerMetricDto(ContainerProduct container) {
        return ContainerMetricDto.builder()
                .id(container.getId())
                .code(container.getCode())
                .productAlias(resolveProductAlias(container.getProductBranch()))
                .sourceMetric(container.getSourceMetric())
                .build();
    }

    private InterfaceMetricDto toInterfaceMetricDto(Interface anInterface) {
        ContainerProduct container = anInterface.getContainerProduct();
        return InterfaceMetricDto.builder()
                .id(anInterface.getId())
                .code(anInterface.getCode())
                .containerCode(container != null ? container.getCode() : null)
                .productAlias(container != null ? resolveProductAlias(container.getProductBranch()) : null)
                .sourceMetric(anInterface.getSourceMetric())
                .build();
    }

    private DiscoveredInterfaceMetricDto toDiscoveredInterfaceMetricDto(DiscoveredInterface discoveredInterface) {
        Product product = discoveredInterface.getProduct();
        return DiscoveredInterfaceMetricDto.builder()
                .id(discoveredInterface.getId())
                .externalId(discoveredInterface.getExternalId())
                .name(discoveredInterface.getName())
                .source(discoveredInterface.getSource())
                .productAlias(product != null ? product.getAlias() : null)
                .sourceMetric(discoveredInterface.getSourceMetric())
                .build();
    }

    private static String resolveProductAlias(ProductBranch productBranch) {
        return productBranch != null ? productBranch.getAlias() : null;
    }

    public void updateSourceMetric(String entity, Long id, String sourceMetric) {
        if (entity == null || entity.isBlank() || id == null || sourceMetric == null || sourceMetric.isEmpty()) {
            throw new IllegalArgumentException("Неверные входные данные");
        }

        String normalizedEntity = entity.trim().toLowerCase();

        switch (normalizedEntity) {
            case "product":
                productRepository.findById(id.intValue()).orElseThrow(() -> new EntityNotFoundException("Ресурс не найден"));
                productRepository.updateSourceMetricById(id.intValue(), sourceMetric);
                break;
            case "container":
                containerRepository.findById(id.intValue()).orElseThrow(() -> new EntityNotFoundException("Ресурс не найден"));
                containerRepository.updateSourceMetricById(id.intValue(), sourceMetric);
                break;
            case "interface":
                interfaceRepository.findById(id.intValue()).orElseThrow(() -> new EntityNotFoundException("Ресурс не найден"));
                interfaceRepository.updateSourceMetricById(id.intValue(), sourceMetric);
                break;
            case "discovered-interface":
                discoveredInterfaceRepository.findById(id.intValue())
                        .orElseThrow(() -> new EntityNotFoundException("Ресурс не найден"));
                discoveredInterfaceRepository.updateSourceMetricById(id.intValue(), sourceMetric);
                break;
            default:
                throw new IllegalArgumentException("Неверные входные данные: " + entity);
        }
    }
}