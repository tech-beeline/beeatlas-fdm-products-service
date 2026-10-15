/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eOperationDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.mapper.E2eGroupedByE2eMapper;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class E2eGroupedByE2eService {

    private static final String MISSING_PRODUCT_KEY = "Не передан идентификатор или alias продукта";
    private static final String BOTH_PRODUCT_KEYS = "Нужно передать либо идентификатор, либо alias продукта";
    private static final String INVALID_PRODUCT_ID = "Идентификатор продукта должен быть положительным числом";
    private static final String EMPTY_ALIAS = "Параметр alias не может быть пустым";
    private static final String PRODUCT_NOT_FOUND = "Продукт не найден.";
    private static final String CAPABILITY_UNAVAILABLE = "Сервис capability недоступен";

    private final ProductRepository productRepository;
    private final E2eRepository e2eRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final OperationRelationRepository operationRelationRepository;
    private final OperationRepository operationRepository;
    private final CapabilityClient capabilityClient;
    private final E2eGroupedByE2eMapper mapper;
    private final E2eOwnerService e2eOwnerService;

    @Transactional(readOnly = true)
    public List<E2eGroupedByE2eDTO> findGroupedByE2e(String id, String alias) {
        Product product = resolveProduct(id, alias);
        log.info("grouped-by-e2e: продукт id={}, alias={}", product.getId(), product.getAlias());

        Set<String> e2eSources = uniqueSourcesIgnoreCase(e2eRepository.findAllSources());
        if (e2eSources.isEmpty()) {
            log.info("grouped-by-e2e: источников e2e нет — пустой ответ");
            return List.of();
        }

        List<DiscoveredInterface> interfaces = discoveredInterfaceRepository
                .findActiveByProductIdAndSourceInIgnoreCase(product.getId(), e2eSources);
        if (interfaces.isEmpty()) {
            log.info("grouped-by-e2e: подходящих интерфейсов нет — пустой ответ");
            return List.of();
        }

        List<Integer> interfaceIds = interfaces.stream().map(DiscoveredInterface::getId).toList();
        List<DiscoveredOperation> discoveredOperations = discoveredOperationRepository
                .findAllByInterfaceIdInAndDeletedDateIsNull(interfaceIds);
        if (discoveredOperations.isEmpty()) {
            log.info("grouped-by-e2e: живых discovered_operation нет — пустой ответ");
            return List.of();
        }

        Map<Integer, DiscoveredOperation> operationsById = discoveredOperations.stream()
                .collect(Collectors.toMap(DiscoveredOperation::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        List<Integer> operationIds = new ArrayList<>(operationsById.keySet());

        List<OperationRelation> relations = operationRelationRepository
                .findDiscoveredOperationParticipations(operationIds);
        if (relations.isEmpty()) {
            log.info("grouped-by-e2e: участий в operation_relations нет — пустой ответ");
            return List.of();
        }

        Map<Integer, Set<Integer>> operationIdsByE2e = groupParticipatingOperationIds(relations, operationsById.keySet());
        if (operationIdsByE2e.isEmpty()) {
            return List.of();
        }

        Map<Integer, E2e> e2eById = e2eRepository.findAllByIdIn(operationIdsByE2e.keySet()).stream()
                .collect(Collectors.toMap(E2e::getId, Function.identity(), (left, right) -> left));

        Map<Integer, Set<Integer>> responseOperationIdsByE2e = new TreeMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : operationIdsByE2e.entrySet()) {
            if (!e2eById.containsKey(entry.getKey()) || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            responseOperationIdsByE2e.put(entry.getKey(), entry.getValue());
        }
        if (responseOperationIdsByE2e.isEmpty()) {
            log.info("grouped-by-e2e: нет карточек e2e для найденных шагов — пустой ответ");
            return List.of();
        }

        Set<Integer> participatingOperationIds = responseOperationIdsByE2e.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(TreeSet::new));

        Map<Integer, Operation> archOperations = loadArchOperations(participatingOperationIds, operationsById);
        Map<Integer, TcDTO> techCapabilities = loadTechCapabilities(archOperations);
        Map<Integer, String> ownerAliases = e2eOwnerService.aliasesOf(e2eById.values());

        List<E2eGroupedByE2eDTO> result = new ArrayList<>();
        for (Map.Entry<Integer, Set<Integer>> entry : responseOperationIdsByE2e.entrySet()) {
            E2e e2e = e2eById.get(entry.getKey());
            List<E2eGroupedByE2eOperationDTO> operations = entry.getValue().stream()
                    .sorted()
                    .map(operationsById::get)
                    .filter(Objects::nonNull)
                    .map(op -> mapper.toOperationDto(op, archOperations, techCapabilities))
                    .toList();
            if (operations.isEmpty()) {
                continue;
            }
            result.add(mapper.toE2eDto(e2e, operations,
                    e2e.getProductId() == null ? null : ownerAliases.get(e2e.getProductId())));
        }
        log.info("grouped-by-e2e: ответ карточек={}", result.size());
        return result;
    }

    private Product resolveProduct(String id, String alias) {
        boolean hasId = id != null;
        boolean hasAlias = alias != null;
        if (!hasId && !hasAlias) {
            throw new IllegalArgumentException(MISSING_PRODUCT_KEY);
        }
        if (hasId && hasAlias) {
            throw new IllegalArgumentException(BOTH_PRODUCT_KEYS);
        }
        if (hasId) {
            Integer productId = parsePositiveId(id);
            return productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException(PRODUCT_NOT_FOUND));
        }
        String trimmed = alias.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException(EMPTY_ALIAS);
        }
        Product product = productRepository.findByAliasCaseInsensitive(trimmed);
        if (product == null) {
            throw new EntityNotFoundException(PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private Integer parsePositiveId(String id) {
        String trimmed = id.trim();
        if (!trimmed.matches("\\d+")) {
            throw new IllegalArgumentException(INVALID_PRODUCT_ID);
        }
        try {
            int value = Integer.parseInt(trimmed);
            if (value <= 0) {
                throw new IllegalArgumentException(INVALID_PRODUCT_ID);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(INVALID_PRODUCT_ID);
        }
    }

    private Set<String> uniqueSourcesIgnoreCase(List<String> sources) {
        Set<String> result = new HashSet<>();
        if (sources == null) {
            return result;
        }
        for (String source : sources) {
            if (source == null) {
                continue;
            }
            String trimmed = source.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            result.add(trimmed.toLowerCase(Locale.ROOT));
        }
        return result;
    }

    private Map<Integer, Set<Integer>> groupParticipatingOperationIds(List<OperationRelation> relations,
                                                                      Set<Integer> candidateIds) {
        Map<Integer, Set<Integer>> byE2e = new TreeMap<>();
        for (OperationRelation relation : relations) {
            if (relation.getE2eId() == null) {
                continue;
            }
            // SFDM-4244: обе стороны связи — id discovered_operation, фильтра по типу нет.
            Set<Integer> ids = byE2e.computeIfAbsent(relation.getE2eId(), key -> new TreeSet<>());
            if (relation.getOperationId() != null && candidateIds.contains(relation.getOperationId())) {
                ids.add(relation.getOperationId());
            }
            if (relation.getRelatedOperationId() != null && candidateIds.contains(relation.getRelatedOperationId())) {
                ids.add(relation.getRelatedOperationId());
            }
        }
        byE2e.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return byE2e;
    }

    private Map<Integer, Operation> loadArchOperations(Set<Integer> participatingOperationIds,
                                                       Map<Integer, DiscoveredOperation> operationsById) {
        Set<Integer> connectionIds = participatingOperationIds.stream()
                .map(operationsById::get)
                .filter(Objects::nonNull)
                .map(DiscoveredOperation::getConnectionOperationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (connectionIds.isEmpty()) {
            return Map.of();
        }
        return operationRepository.findAllByIdIn(new ArrayList<>(connectionIds)).stream()
                .collect(Collectors.toMap(Operation::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Integer, TcDTO> loadTechCapabilities(Map<Integer, Operation> archOperations) {
        List<Integer> tcIds = archOperations.values().stream()
                .map(Operation::getTcId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (tcIds.isEmpty()) {
            log.info("grouped-by-e2e: ТС не запрашиваются — нет tc_id");
            return Map.of();
        }
        log.info("grouped-by-e2e: запрос ТС в capability, count={}, tcIds={}", tcIds.size(), tcIds);
        List<TcDTO> tcs;
        try {
            tcs = capabilityClient.getTcs(tcIds);
        } catch (RuntimeException e) {
            log.error("grouped-by-e2e: ошибка capability: {}", e.getMessage(), e);
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, CAPABILITY_UNAVAILABLE);
        }
        if (tcs == null || tcs.isEmpty()) {
            log.error("grouped-by-e2e: пустой ответ capability при непустом запросе");
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, CAPABILITY_UNAVAILABLE);
        }
        return tcs.stream()
                .filter(tc -> tc != null && tc.getId() != null)
                .collect(Collectors.toMap(TcDTO::getId, Function.identity(), (left, right) -> left));
    }
}
