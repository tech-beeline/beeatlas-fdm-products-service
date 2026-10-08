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
import ru.beeline.fdmproducts.client.ProjectClient;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eOperationDTO;
import ru.beeline.fdmproducts.dto.project.UseCaseOperationsGroupedDTO;
import ru.beeline.fdmproducts.dto.usecase.UseCaseGroupedByUseCaseDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.mapper.E2eGroupedByE2eMapper;
import ru.beeline.fdmproducts.mapper.UseCaseGroupedByUseCaseMapper;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UseCaseGroupedByUseCaseService {

    private static final String MISSING_PRODUCT_KEY = "Не передан идентификатор или alias продукта";
    private static final String BOTH_PRODUCT_KEYS = "Нужно передать либо идентификатор, либо alias продукта";
    private static final String INVALID_PRODUCT_ID = "Идентификатор продукта должен быть положительным числом";
    private static final String EMPTY_ALIAS = "Параметр alias не может быть пустым";
    private static final String PRODUCT_NOT_FOUND = "Продукт не найден.";
    private static final String CAPABILITY_UNAVAILABLE = "Сервис capability недоступен";

    private final ProductRepository productRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final OperationRepository operationRepository;
    private final ProjectClient projectClient;
    private final CapabilityClient capabilityClient;
    private final E2eGroupedByE2eMapper operationMapper;
    private final UseCaseGroupedByUseCaseMapper useCaseMapper;

    @Transactional(readOnly = true)
    public List<UseCaseGroupedByUseCaseDTO> findGroupedByUseCase(String id, String alias) {
        Product product = resolveProduct(id, alias);
        log.info("grouped-by-use-case: продукт id={}, alias={}", product.getId(), product.getAlias());

        List<DiscoveredInterface> interfaces = discoveredInterfaceRepository
                .findAllByProductAndSourceIgnoreCaseAndDeletedDateIsNull(
                        product, UseCaseDiscoveredInterfaceService.SOURCE);
        if (interfaces.isEmpty()) {
            log.info("grouped-by-use-case: интерфейсов UseCase нет — пустой ответ");
            return List.of();
        }

        List<Integer> interfaceIds = interfaces.stream().map(DiscoveredInterface::getId).toList();
        List<DiscoveredOperation> discoveredOperations = discoveredOperationRepository
                .findAllByInterfaceIdInAndDeletedDateIsNull(interfaceIds);
        if (discoveredOperations.isEmpty()) {
            log.info("grouped-by-use-case: живых discovered_operation нет — пустой ответ");
            return List.of();
        }

        Map<Integer, DiscoveredOperation> operationsById = discoveredOperations.stream()
                .collect(Collectors.toMap(DiscoveredOperation::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        List<Integer> operationIds = new ArrayList<>(new TreeSet<>(operationsById.keySet()));
        if (operationIds.isEmpty()) {
            return List.of();
        }

        List<UseCaseOperationsGroupedDTO> projectCards = projectClient.findOperationsGroupedByUseCase(operationIds);
        if (projectCards.isEmpty()) {
            log.info("grouped-by-use-case: project вернул пустой список");
            return List.of();
        }

        Set<Integer> participatingOperationIds = collectParticipatingOperationIds(projectCards, operationsById.keySet());
        Map<Integer, Operation> archOperations = loadArchOperations(participatingOperationIds, operationsById);
        Map<Integer, TcDTO> techCapabilities = loadTechCapabilities(archOperations);

        List<UseCaseGroupedByUseCaseDTO> result = new ArrayList<>();
        for (UseCaseOperationsGroupedDTO card : projectCards) {
            List<E2eGroupedByE2eOperationDTO> operations = buildOperations(card, operationsById,
                    archOperations, techCapabilities);
            result.add(useCaseMapper.toUseCaseDto(card, operations));
        }
        log.info("grouped-by-use-case: ответ карточек={}", result.size());
        return result;
    }

    private List<E2eGroupedByE2eOperationDTO> buildOperations(UseCaseOperationsGroupedDTO card,
                                                              Map<Integer, DiscoveredOperation> operationsById,
                                                              Map<Integer, Operation> archOperations,
                                                              Map<Integer, TcDTO> techCapabilities) {
        if (card.getOperationIds() == null || card.getOperationIds().isEmpty()) {
            return List.of();
        }
        List<E2eGroupedByE2eOperationDTO> operations = new ArrayList<>();
        for (Integer operationId : card.getOperationIds()) {
            DiscoveredOperation discovered = operationsById.get(operationId);
            if (discovered == null) {
                continue;
            }
            operations.add(operationMapper.toOperationDto(discovered, archOperations, techCapabilities));
        }
        return operations;
    }

    private Set<Integer> collectParticipatingOperationIds(List<UseCaseOperationsGroupedDTO> projectCards,
                                                          Set<Integer> candidateIds) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (UseCaseOperationsGroupedDTO card : projectCards) {
            if (card.getOperationIds() == null) {
                continue;
            }
            for (Integer operationId : card.getOperationIds()) {
                if (candidateIds.contains(operationId)) {
                    ids.add(operationId);
                }
            }
        }
        return ids;
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
            log.info("grouped-by-use-case: ТС не запрашиваются — нет tc_id");
            return Map.of();
        }
        log.info("grouped-by-use-case: запрос ТС в capability, count={}, tcIds={}", tcIds.size(), tcIds);
        List<TcDTO> tcs;
        try {
            tcs = capabilityClient.getTcs(tcIds);
        } catch (RuntimeException e) {
            log.error("grouped-by-use-case: ошибка capability: {}", e.getMessage(), e);
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, CAPABILITY_UNAVAILABLE);
        }
        if (tcs == null || tcs.isEmpty()) {
            log.error("grouped-by-use-case: пустой ответ capability при непустом запросе");
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, CAPABILITY_UNAVAILABLE);
        }
        return tcs.stream()
                .filter(tc -> tc != null && tc.getId() != null)
                .collect(Collectors.toMap(TcDTO::getId, Function.identity(), (left, right) -> left));
    }
}
