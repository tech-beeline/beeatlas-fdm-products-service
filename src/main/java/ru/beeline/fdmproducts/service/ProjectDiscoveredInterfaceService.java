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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Синхронизация обнаруженных интерфейсов и операций в контексте проекта (SFDM-4095).
 * <p>
 * Тело запроса — полный состав интерфейсов проекта для одного source, а не приращение к нему:
 * интерфейсы проекта с этим source, которых в теле нет, помечаются удалёнными, и так же
 * трактуется состав operations внутри каждого интерфейса. Ключ интерфейса —
 * (project_id, source, external_id = interfaceCode, product_id), ключ операции —
 * (interface_id, name, type); всё сравнение — без учёта регистра. Ничего не удаляется физически:
 * на discovered_operation.id ссылаются связи use case на стороне сервиса project.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectDiscoveredInterfaceService {

    /**
     * Разделитель частей составного ключа: символ, которого не бывает в external_id / name / type,
     * поэтому склейка «часть + разделитель + часть» однозначна — обычный пробел давал бы совпадение
     * ключей у пар («a b», «c») и («a», «b c»).
     */
    private static final char KEY_SEPARATOR = 0;

    private final ProductRepository productRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final DiscoveredOperationRepository discoveredOperationRepository;

    /** Элемент тела с разрешённым продуктом и ключом синхронизации — (LOWER(interfaceCode), product_id). */
    private record RequestedInterface(ProjectDiscoveredInterfaceDTO dto, String interfaceCode,
                                      Product product, String key) {
    }

    @Transactional
    public List<ProjectDiscoveredInterfaceResultDTO> syncProjectInterfaces(
            Integer projectId, String source, List<ProjectDiscoveredInterfaceDTO> interfaces) {

        if (projectId == null) {
            throw new IllegalArgumentException("Не указан идентификатор проекта");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Параметр source не может быть пустым");
        }
        if (interfaces == null) {
            throw new IllegalArgumentException("Отсутствует тело запроса");
        }
        String requestSource = source.trim();
        log.info("Синхронизация discovered интерфейсов проекта: projectId={}, source={}, интерфейсов в теле={}",
                projectId, requestSource, interfaces.size());

        // Валидация и резолв продуктов — до единой записи в БД: любая ошибка тела откатывает запрос целиком.
        List<RequestedInterface> requested = resolveRequest(interfaces);
        LocalDateTime now = LocalDateTime.now();

        List<DiscoveredInterface> stored =
                discoveredInterfaceRepository.findAllByProjectIdAndSourceIgnoreCase(projectId, requestSource);
        Map<String, DiscoveredInterface> storedByKey = indexInterfaces(stored);
        softDeleteMissingInterfaces(stored, requested, now);

        List<ProjectDiscoveredInterfaceResultDTO> result = new ArrayList<>(requested.size());
        for (RequestedInterface item : requested) {
            DiscoveredInterface discoveredInterface = storedByKey.get(item.key());
            boolean created = discoveredInterface == null;
            if (created) {
                discoveredInterface = discoveredInterfaceRepository.save(DiscoveredInterface.builder()
                        .name(item.interfaceCode())
                        .externalId(item.interfaceCode())
                        .product(item.product())
                        .projectId(projectId)
                        .source(requestSource)
                        .createdDate(now)
                        .build());
                log.info("Создан discovered_interface: id={}, code={}, productId={}",
                        discoveredInterface.getId(), item.interfaceCode(), item.product().getId());
            } else {
                // source / product_id / project_id / external_id остаются прежними — они и есть ключ поиска.
                discoveredInterface.setDeletedDate(null);
                discoveredInterface.setUpdatedDate(now);
                discoveredInterfaceRepository.save(discoveredInterface);
            }
            result.add(ProjectDiscoveredInterfaceResultDTO.builder()
                    .interfaceCode(item.dto().getInterfaceCode())
                    .product(item.dto().getProduct())
                    .operations(syncOperations(discoveredInterface, created, item.dto().getOperations(), now))
                    .build());
        }

        log.info("Синхронизация discovered интерфейсов проекта завершена: projectId={}, интерфейсов={}, операций={}",
                projectId, result.size(), result.stream().mapToInt(item -> item.getOperations().size()).sum());
        return result;
    }

    /** Проверяет тело целиком и разрешает alias продуктов; 400 на дублях и незаполненных полях, 404 на продукте. */
    private List<RequestedInterface> resolveRequest(List<ProjectDiscoveredInterfaceDTO> interfaces) {
        Map<String, Product> productByAlias = new HashMap<>();
        Set<String> seenKeys = new HashSet<>();
        List<RequestedInterface> requested = new ArrayList<>(interfaces.size());
        for (ProjectDiscoveredInterfaceDTO dto : interfaces) {
            if (dto == null) {
                throw new IllegalArgumentException("Пустой элемент в теле запроса");
            }
            String interfaceCode = requireNonBlank(dto.getInterfaceCode(), "interfaceCode");
            String alias = requireNonBlank(dto.getProduct(), "product");
            if (dto.getOperations() == null) {
                throw new IllegalArgumentException(
                        "Отсутствует обязательное поле operations, interfaceCode=" + interfaceCode);
            }
            validateOperations(dto.getOperations(), interfaceCode);

            Product product = resolveProduct(alias, productByAlias);
            String key = interfaceKey(interfaceCode, product.getId());
            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException("Интерфейс встречается в теле запроса дважды: interfaceCode="
                        + interfaceCode + ", product=" + alias);
            }
            requested.add(new RequestedInterface(dto, interfaceCode, product, key));
        }
        return requested;
    }

    private void validateOperations(List<ProjectDiscoveredOperationDTO> operations, String interfaceCode) {
        Set<String> seenKeys = new HashSet<>();
        for (ProjectDiscoveredOperationDTO dto : operations) {
            if (dto == null) {
                throw new IllegalArgumentException("Пустой элемент в operations, interfaceCode=" + interfaceCode);
            }
            String name = requireNonBlank(dto.getName(), "operations.name");
            String type = requireNonBlank(dto.getType(), "operations.type");
            if (!seenKeys.add(operationKey(name, type))) {
                throw new IllegalArgumentException("Операция встречается в operations дважды: name=" + name
                        + ", type=" + type + ", interfaceCode=" + interfaceCode);
            }
        }
    }

    /**
     * Индекс сохранённых интерфейсов по ключу синхронизации. Дубли по ключу в БД возможны на данных,
     * заведённых до появления этого метода; из них выигрывает активная запись, чтобы повторный вызов
     * не оживлял удалённую копию рядом с живой.
     */
    private Map<String, DiscoveredInterface> indexInterfaces(List<DiscoveredInterface> stored) {
        Map<String, DiscoveredInterface> byKey = new HashMap<>();
        for (DiscoveredInterface item : stored) {
            String key = interfaceKey(item.getExternalId(), productId(item));
            DiscoveredInterface kept = byKey.get(key);
            if (kept == null || (kept.getDeletedDate() != null && item.getDeletedDate() == null)) {
                byKey.put(key, item);
            }
        }
        return byKey;
    }

    private void softDeleteMissingInterfaces(List<DiscoveredInterface> stored, List<RequestedInterface> requested,
                                             LocalDateTime now) {
        Set<String> requestKeys = new HashSet<>();
        for (RequestedInterface item : requested) {
            requestKeys.add(item.key());
        }
        for (DiscoveredInterface item : stored) {
            if (item.getDeletedDate() != null || requestKeys.contains(interfaceKey(item.getExternalId(), productId(item)))) {
                continue;
            }
            // Операции такого интерфейса отдельно не помечаем: интерфейс целиком помечен удалённым.
            item.setDeletedDate(now);
            item.setUpdatedDate(now);
            discoveredInterfaceRepository.save(item);
            log.info("Интерфейс проекта отсутствует в теле — помечен удалённым: id={}, externalId={}",
                    item.getId(), item.getExternalId());
        }
    }

    /**
     * Приводит состав операций интерфейса к телу запроса. Для только что созданного интерфейса
     * (freshInterface) в БД по определению ничего нет — запрос за существующими операциями пропускается.
     */
    private List<ProjectDiscoveredOperationResultDTO> syncOperations(DiscoveredInterface discoveredInterface,
                                                                     boolean freshInterface,
                                                                     List<ProjectDiscoveredOperationDTO> dtos,
                                                                     LocalDateTime now) {
        List<DiscoveredOperation> stored = freshInterface
                ? List.of()
                : discoveredOperationRepository.findAllByInterfaceId(discoveredInterface.getId());
        Map<String, DiscoveredOperation> storedByKey = new HashMap<>();
        for (DiscoveredOperation item : stored) {
            String key = operationKey(item.getName(), item.getType());
            DiscoveredOperation kept = storedByKey.get(key);
            if (kept == null || (kept.getDeletedDate() != null && item.getDeletedDate() == null)) {
                storedByKey.put(key, item);
            }
        }

        Set<String> requestKeys = new HashSet<>();
        List<ProjectDiscoveredOperationResultDTO> result = new ArrayList<>(dtos.size());
        for (ProjectDiscoveredOperationDTO dto : dtos) {
            String name = dto.getName().trim();
            String type = dto.getType().trim();
            String key = operationKey(name, type);
            requestKeys.add(key);

            DiscoveredOperation operation = storedByKey.get(key);
            if (operation == null) {
                operation = discoveredOperationRepository.save(DiscoveredOperation.builder()
                        .discoveredInterface(discoveredInterface)
                        .name(name)
                        .type(type)
                        .description(dto.getDescriptionOperation())
                        .tcCode(dto.getTcCode())
                        .tcDescription(dto.getDescriptionTc())
                        .createdDate(now)
                        .build());
            } else {
                refreshOperation(operation, dto, now);
            }
            result.add(ProjectDiscoveredOperationResultDTO.builder()
                    .discoveredOperationId(operation.getId())
                    .name(dto.getName())
                    .type(dto.getType())
                    .build());
        }

        for (DiscoveredOperation item : stored) {
            if (item.getDeletedDate() != null || requestKeys.contains(operationKey(item.getName(), item.getType()))) {
                continue;
            }
            item.setDeletedDate(now);
            item.setUpdatedDate(now);
            discoveredOperationRepository.save(item);
            log.info("Операция отсутствует в теле — помечена удалённой: id={}, name={}, type={}",
                    item.getId(), item.getName(), item.getType());
        }
        return result;
    }

    /**
     * Тело несёт полный снимок атрибутов операции, поэтому отсутствующее поле — это NULL, а не
     * «оставить как было»: иначе снятое в источнике описание навсегда осталось бы в каталоге.
     * updated_date двигаем только при реальном изменении.
     */
    private void refreshOperation(DiscoveredOperation operation, ProjectDiscoveredOperationDTO dto,
                                  LocalDateTime now) {
        boolean changed = false;
        if (!Objects.equals(operation.getDescription(), dto.getDescriptionOperation())) {
            operation.setDescription(dto.getDescriptionOperation());
            changed = true;
        }
        if (!Objects.equals(operation.getTcCode(), dto.getTcCode())) {
            operation.setTcCode(dto.getTcCode());
            changed = true;
        }
        if (!Objects.equals(operation.getTcDescription(), dto.getDescriptionTc())) {
            operation.setTcDescription(dto.getDescriptionTc());
            changed = true;
        }
        if (operation.getDeletedDate() != null) {
            operation.setDeletedDate(null);
            changed = true;
        }
        if (changed) {
            operation.setUpdatedDate(now);
            discoveredOperationRepository.save(operation);
        }
    }

    private Product resolveProduct(String alias, Map<String, Product> productByAlias) {
        String cacheKey = alias.toLowerCase(Locale.ROOT);
        Product cached = productByAlias.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Product product = productRepository.findByAliasCaseInsensitive(alias);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с alias '" + alias + "' не найден");
        }
        productByAlias.put(cacheKey, product);
        return product;
    }

    private Integer productId(DiscoveredInterface discoveredInterface) {
        return discoveredInterface.getProduct() == null ? null : discoveredInterface.getProduct().getId();
    }

    private String interfaceKey(String externalId, Integer productId) {
        return lower(externalId) + KEY_SEPARATOR + productId;
    }

    private String operationKey(String name, String type) {
        return lower(name) + KEY_SEPARATOR + lower(type);
    }

    private String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Отсутствует обязательное поле " + field);
        }
        return value.trim();
    }
}
