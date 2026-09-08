/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.discovered.ProjectDiscoveredInterfaceDTO;
import ru.beeline.fdmproducts.dto.discovered.ProjectDiscoveredInterfaceResultDTO;
import ru.beeline.fdmproducts.dto.discovered.ProjectDiscoveredOperationDTO;
import ru.beeline.fdmproducts.dto.discovered.ProjectDiscoveredOperationResultDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Upsert обнаруженных интерфейсов и операций, публикуемых из задачи проекта (SFDM-4081).
 * Ключ интерфейса — (source, product_id, external_id = interfaceCode), ключ операции —
 * (interface_id, name, type). Метод ничего не удаляет: состав use case хранится на стороне
 * сервиса project, здесь только пополняется каталог обнаруженных сущностей.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectDiscoveredInterfaceService {

    private final ProductRepository productRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final DiscoveredOperationRepository discoveredOperationRepository;

    @Transactional
    public List<ProjectDiscoveredInterfaceResultDTO> upsertProjectInterfaces(
            Integer projectId, String source, List<ProjectDiscoveredInterfaceDTO> interfaces) {

        if (projectId == null) {
            throw new IllegalArgumentException("Не указан projectId");
        }
        if (interfaces == null || interfaces.isEmpty()) {
            throw new IllegalArgumentException("Отсутствует непустое тело запроса");
        }
        String interfaceSource = (source == null || source.isBlank()) ? "ProjectTask" : source.trim();
        log.info("ProjectTask discovered upsert: обработка, projectId={}, source={}, интерфейсов={}",
                projectId, interfaceSource, interfaces.size());

        Map<String, Product> productByAlias = new HashMap<>();
        List<ProjectDiscoveredInterfaceResultDTO> result = new ArrayList<>();
        for (ProjectDiscoveredInterfaceDTO dto : interfaces) {
            result.add(upsertInterface(projectId, interfaceSource, dto, productByAlias));
        }

        log.info("ProjectTask discovered upsert: завершён, projectId={}, интерфейсов={}, операций={}",
                projectId, result.size(), result.stream().mapToInt(item -> item.getOperations().size()).sum());
        return result;
    }

    private ProjectDiscoveredInterfaceResultDTO upsertInterface(Integer projectId, String source,
                                                                ProjectDiscoveredInterfaceDTO dto,
                                                                Map<String, Product> productByAlias) {
        if (dto == null) {
            throw new IllegalArgumentException("Пустой элемент в теле запроса");
        }
        String interfaceCode = requireNonBlank(dto.getInterfaceCode(), "interfaceCode");
        String alias = requireNonBlank(dto.getProduct(), "product");
        if (dto.getOperations() == null || dto.getOperations().isEmpty()) {
            throw new IllegalArgumentException("Отсутствует обязательное непустое поле operations, "
                    + "interfaceCode=" + interfaceCode);
        }

        Product product = resolveProduct(alias, productByAlias);
        DiscoveredInterface discoveredInterface = discoveredInterfaceRepository
                .findBySourceAndProductIdAndExternalIdIgnoreCase(source, product.getId(), interfaceCode)
                .orElse(null);
        if (discoveredInterface == null) {
            discoveredInterface = discoveredInterfaceRepository.save(DiscoveredInterface.builder()
                    .name(interfaceCode)
                    .externalId(interfaceCode)
                    .product(product)
                    .projectId(projectId)
                    .source(source)
                    .createdDate(LocalDateTime.now())
                    .build());
            log.info("ProjectTask discovered upsert: создан discovered_interface, id={}, code={}, productId={}",
                    discoveredInterface.getId(), interfaceCode, product.getId());
        } else {
            refreshInterface(discoveredInterface, projectId);
        }

        List<ProjectDiscoveredOperationResultDTO> operations = new ArrayList<>();
        for (ProjectDiscoveredOperationDTO operationDto : dto.getOperations()) {
            operations.add(upsertOperation(discoveredInterface, operationDto));
        }
        return ProjectDiscoveredInterfaceResultDTO.builder()
                .id(discoveredInterface.getId())
                .interfaceCode(dto.getInterfaceCode())
                .product(dto.getProduct())
                .operations(operations)
                .build();
    }

    private void refreshInterface(DiscoveredInterface discoveredInterface, Integer projectId) {
        boolean update = false;
        if (!Objects.equals(discoveredInterface.getProjectId(), projectId)) {
            discoveredInterface.setProjectId(projectId);
            update = true;
        }
        if (discoveredInterface.getDeletedDate() != null) {
            discoveredInterface.setDeletedDate(null);
            update = true;
        }
        if (update) {
            discoveredInterface.setUpdatedDate(LocalDateTime.now());
            discoveredInterfaceRepository.save(discoveredInterface);
            log.info("ProjectTask discovered upsert: обновлён discovered_interface, id={}",
                    discoveredInterface.getId());
        }
    }

    private ProjectDiscoveredOperationResultDTO upsertOperation(DiscoveredInterface discoveredInterface,
                                                                ProjectDiscoveredOperationDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Пустой элемент в operations");
        }
        String name = requireNonBlank(dto.getName(), "operations.name");
        String type = requireNonBlank(dto.getType(), "operations.type");

        DiscoveredOperation operation = discoveredOperationRepository
                .findByInterfaceIdAndNameAndTypeAllIgnoreCase(discoveredInterface.getId(), name, type)
                .orElse(null);
        if (operation == null) {
            operation = discoveredOperationRepository.save(DiscoveredOperation.builder()
                    .discoveredInterface(discoveredInterface)
                    .name(name)
                    .type(type)
                    .description(dto.getDescriptionOperation())
                    .tcCode(dto.getTcCode())
                    .tcDescription(dto.getDescriptionTc())
                    .createdDate(LocalDateTime.now())
                    .build());
            log.info("ProjectTask discovered upsert: создана discovered_operation, id={}, name={}, type={}",
                    operation.getId(), name, type);
        } else {
            refreshOperation(operation, dto);
        }
        return ProjectDiscoveredOperationResultDTO.builder()
                .id(operation.getId())
                .name(dto.getName())
                .type(dto.getType())
                .build();
    }

    /** Присланные значения перезаписывают сохранённые; не переданные (null) — оставляют прежние. */
    private void refreshOperation(DiscoveredOperation operation, ProjectDiscoveredOperationDTO dto) {
        boolean update = false;
        if (dto.getDescriptionOperation() != null
                && !Objects.equals(operation.getDescription(), dto.getDescriptionOperation())) {
            operation.setDescription(dto.getDescriptionOperation());
            update = true;
        }
        if (dto.getTcCode() != null && !Objects.equals(operation.getTcCode(), dto.getTcCode())) {
            operation.setTcCode(dto.getTcCode());
            update = true;
        }
        if (dto.getDescriptionTc() != null
                && !Objects.equals(operation.getTcDescription(), dto.getDescriptionTc())) {
            operation.setTcDescription(dto.getDescriptionTc());
            update = true;
        }
        if (operation.getDeletedDate() != null) {
            operation.setDeletedDate(null);
            update = true;
        }
        if (update) {
            operation.setUpdatedDate(LocalDateTime.now());
            discoveredOperationRepository.save(operation);
            log.info("ProjectTask discovered upsert: обновлена discovered_operation, id={}", operation.getId());
        }
    }

    private Product resolveProduct(String alias, Map<String, Product> productByAlias) {
        Product cached = productByAlias.get(alias.toLowerCase(Locale.ROOT));
        if (cached != null) {
            return cached;
        }
        Product product = productRepository.findByAliasCaseInsensitive(alias);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с указанным alias не найден: " + alias);
        }
        productByAlias.put(alias.toLowerCase(Locale.ROOT), product);
        return product;
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Отсутствует обязательное поле " + field);
        }
        return value.trim();
    }
}
