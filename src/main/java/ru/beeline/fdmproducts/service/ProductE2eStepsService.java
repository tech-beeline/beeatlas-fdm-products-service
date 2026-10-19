/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.e2e.ProductE2eCardDTO;
import ru.beeline.fdmproducts.dto.e2e.ProductE2eOperationDTO;
import ru.beeline.fdmproducts.dto.e2e.ProductE2eStepDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductE2eStepsService {

    private static final String OPERATION = "operation";
    private static final String DISCOVERED_OPERATION = "discovered_operation";

    private final ProductRepository productRepository;
    private final ProductBranchService productBranchService;
    private final ContainerRepository containerRepository;
    private final InterfaceRepository interfaceRepository;
    private final OperationRepository operationRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final OperationRelationRepository operationRelationRepository;
    private final E2eRepository e2eRepository;
    private final OperationTechCapabilityResolver techCapabilityResolver;
    private final E2eOwnerService e2eOwnerService;

    public List<ProductE2eStepDTO> getE2eSteps(String cmdb) {
        String decodedCmdb = URLDecoder.decode(cmdb, StandardCharsets.UTF_8);
        Product product = productRepository.findByAliasCaseInsensitive(decodedCmdb);
        if (product == null) {
            throw new EntityNotFoundException("Продукт с данным cmdb: " + decodedCmdb + " не найден.");
        }

        Map<Integer, Operation> operations = architectureOperationsOf(product);
        Map<Integer, DiscoveredOperation> discoveredOperations = discoveredOperationsOf(product);
        if (operations.isEmpty() && discoveredOperations.isEmpty()) {
            log.info("E2E-шаги продукта: методов нет, cmdb={}", decodedCmdb);
            return List.of();
        }

        Map<Integer, List<MethodRef>> methodsByE2e = methodsByE2e(operations, discoveredOperations);
        if (methodsByE2e.isEmpty()) {
            log.info("E2E-шаги продукта: методы не участвуют в E2E, cmdb={}", decodedCmdb);
            return List.of();
        }

        Map<Integer, Operation> connectedOperations = connectedOperationsOf(discoveredOperations.values());
        Map<Integer, TcDTO> techCapabilities = techCapabilityResolver.load(
                Stream.concat(operations.values().stream(), connectedOperations.values().stream()).toList());

        List<E2e> e2es = e2eRepository.findAllById(methodsByE2e.keySet());
        Map<Integer, String> ownerAliases = e2eOwnerService.aliasesOf(e2es);
        List<ProductE2eStepDTO> steps = e2es.stream()
                .map(e2e -> new ProductE2eStepDTO(card(e2e, ownerAliases), methodsByE2e.get(e2e.getId()).stream()
                        .map(method -> operationDto(method, operations, discoveredOperations, connectedOperations,
                                techCapabilities))
                        .filter(Objects::nonNull)
                        .toList()))
                .sorted(Comparator.comparing((ProductE2eStepDTO step) -> step.getE2e().getBiStepCode(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(step -> step.getE2e().getCode(), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        log.info("E2E-шаги продукта: cmdb={}, e2e={}", decodedCmdb, steps.size());
        return steps;
    }

    private Map<Integer, Operation> architectureOperationsOf(Product product) {
        List<Integer> containerIds = productBranchService.find(product.getAlias(), null)
                .map(branch -> containerRepository.findAllByProductBranchIdAndDeletedDateIsNull(branch.getId()))
                .orElse(List.of())
                .stream()
                .map(ContainerProduct::getId)
                .toList();
        if (containerIds.isEmpty()) {
            return Map.of();
        }
        List<Integer> interfaceIds = interfaceRepository.findAllByContainerIdInAndDeletedDateIsNull(containerIds)
                .stream()
                .map(Interface::getId)
                .toList();
        if (interfaceIds.isEmpty()) {
            return Map.of();
        }
        return operationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(interfaceIds).stream()
                .collect(Collectors.toMap(Operation::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Integer, DiscoveredOperation> discoveredOperationsOf(Product product) {
        List<Integer> interfaceIds = discoveredInterfaceRepository.findAllByProductAndDeletedDateIsNull(product)
                .stream()
                .map(DiscoveredInterface::getId)
                .toList();
        if (interfaceIds.isEmpty()) {
            return Map.of();
        }
        return discoveredOperationRepository.findAllByInterfaceIdInAndDeletedDateIsNull(interfaceIds).stream()
                .collect(Collectors.toMap(DiscoveredOperation::getId, Function.identity(), (left, right) -> left,
                        LinkedHashMap::new));
    }

    /**
     * SFDM-4244: обе стороны связи — id discovered_operation. Discovered-метод продукта попадает в шаг,
     * если стоит на любой стороне связи. Архитектурный метод продукта попадает в шаг, если
     * connection_operation_id обнаруженного метода этой связи равен id метода.
     */
    private Map<Integer, List<MethodRef>> methodsByE2e(Map<Integer, Operation> operations,
                                                       Map<Integer, DiscoveredOperation> discoveredOperations) {
        Map<Integer, DiscoveredOperation> connectedToProduct = operations.isEmpty() ? Map.of()
                : discoveredOperationRepository.findAllByConnectionOperationIdInAndDeletedDateIsNull(operations.keySet())
                .stream()
                .collect(Collectors.toMap(DiscoveredOperation::getId, Function.identity(), (left, right) -> left));
        List<Integer> participantIds = Stream.concat(discoveredOperations.keySet().stream(),
                        connectedToProduct.keySet().stream())
                .distinct()
                .toList();
        if (participantIds.isEmpty()) {
            return Map.of();
        }

        List<MethodOccurrence> occurrences = new ArrayList<>();
        for (OperationRelation relation : operationRelationRepository.findDiscoveredOperationParticipations(participantIds)) {
            for (Integer side : java.util.Arrays.asList(relation.getRelatedOperationId(), relation.getOperationId())) {
                if (side == null) {
                    continue;
                }
                if (discoveredOperations.containsKey(side)) {
                    occurrences.add(new MethodOccurrence(relation.getE2eId(), relation.getOrder(), relation.getId(),
                            new MethodRef(DISCOVERED_OPERATION, side)));
                }
                DiscoveredOperation discovered = discoveredOperations.containsKey(side)
                        ? discoveredOperations.get(side) : connectedToProduct.get(side);
                Integer connectionId = discovered == null ? null : discovered.getConnectionOperationId();
                if (connectionId != null && operations.containsKey(connectionId)) {
                    occurrences.add(new MethodOccurrence(relation.getE2eId(), relation.getOrder(), relation.getId(),
                            new MethodRef(OPERATION, connectionId)));
                }
            }
        }
        return occurrences.stream()
                .sorted(Comparator.comparing(MethodOccurrence::order, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MethodOccurrence::relationId))
                .collect(Collectors.groupingBy(MethodOccurrence::e2eId, LinkedHashMap::new,
                        Collectors.mapping(MethodOccurrence::method,
                                Collectors.collectingAndThen(Collectors.toList(),
                                        methods -> methods.stream().distinct().toList()))));
    }

    private Map<Integer, Operation> connectedOperationsOf(Iterable<DiscoveredOperation> discoveredOperations) {
        List<Integer> ids = new ArrayList<>();
        discoveredOperations.forEach(operation -> {
            if (operation.getConnectionOperationId() != null && !ids.contains(operation.getConnectionOperationId())) {
                ids.add(operation.getConnectionOperationId());
            }
        });
        if (ids.isEmpty()) {
            return Map.of();
        }
        return operationRepository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(Operation::getId, Function.identity(), (left, right) -> left));
    }

    private static ProductE2eOperationDTO operationDto(MethodRef method, Map<Integer, Operation> operations,
                                                       Map<Integer, DiscoveredOperation> discoveredOperations,
                                                       Map<Integer, Operation> connectedOperations,
                                                       Map<Integer, TcDTO> techCapabilities) {
        if (DISCOVERED_OPERATION.equals(method.entityType())) {
            DiscoveredOperation discovered = discoveredOperations.get(method.id());
            if (discovered == null) {
                return null;
            }
            Operation connected = discovered.getConnectionOperationId() == null ? null
                    : connectedOperations.get(discovered.getConnectionOperationId());
            return new ProductE2eOperationDTO(discovered.getId(), DISCOVERED_OPERATION, discovered.getType(),
                    discovered.getName(), OperationTechCapabilityResolver.of(discovered, connected, techCapabilities));
        }
        Operation operation = operations.get(method.id());
        if (operation == null) {
            return null;
        }
        return new ProductE2eOperationDTO(operation.getId(), OPERATION, operation.getType(), operation.getName(),
                OperationTechCapabilityResolver.of(operation, techCapabilities));
    }

    private static ProductE2eCardDTO card(E2e e2e, Map<Integer, String> ownerAliases) {
        return new ProductE2eCardDTO(e2e.getId(), e2e.getCode(), e2e.getName(), e2e.getBiStepCode(), e2e.getSource(),
                e2e.getProductId(), e2e.getProductId() == null ? null : ownerAliases.get(e2e.getProductId()));
    }

    private record MethodRef(String entityType, Integer id) {
    }

    private record MethodOccurrence(Integer e2eId, Integer order, Integer relationId, MethodRef method) {
    }
}
