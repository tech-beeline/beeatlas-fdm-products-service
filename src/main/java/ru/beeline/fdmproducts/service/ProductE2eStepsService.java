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

        Map<Integer, List<MethodRef>> methodsByE2e = methodsByE2e(operations.keySet().stream().toList(),
                discoveredOperations.keySet().stream().toList());
        if (methodsByE2e.isEmpty()) {
            log.info("E2E-шаги продукта: методы не участвуют в E2E, cmdb={}", decodedCmdb);
            return List.of();
        }

        Map<Integer, Operation> connectedOperations = connectedOperationsOf(discoveredOperations.values());
        Map<Integer, TcDTO> techCapabilities = techCapabilityResolver.load(
                Stream.concat(operations.values().stream(), connectedOperations.values().stream()).toList());

        List<ProductE2eStepDTO> steps = e2eRepository.findAllById(methodsByE2e.keySet()).stream()
                .map(e2e -> new ProductE2eStepDTO(card(e2e), methodsByE2e.get(e2e.getId()).stream()
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

    private Map<Integer, List<MethodRef>> methodsByE2e(List<Integer> operationIds, List<Integer> discoveredIds) {
        List<MethodOccurrence> occurrences = new ArrayList<>();
        relations(operationIds, discoveredIds).forEach(relation -> {
            MethodRef callee = new MethodRef(relation.getEntityTypeOperationRelation(),
                    relation.getRelatedOperationId());
            if (belongsToProduct(callee, operationIds, discoveredIds)) {
                occurrences.add(new MethodOccurrence(relation.getE2eId(), relation.getOrder(), relation.getId(),
                        callee));
            }
            MethodRef caller = new MethodRef(relation.getEntityTypeOperation(), relation.getOperationId());
            if (belongsToProduct(caller, operationIds, discoveredIds)) {
                occurrences.add(new MethodOccurrence(relation.getE2eId(), relation.getOrder(), relation.getId(),
                        caller));
            }
        });
        return occurrences.stream()
                .sorted(Comparator.comparing(MethodOccurrence::order, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MethodOccurrence::relationId))
                .collect(Collectors.groupingBy(MethodOccurrence::e2eId, LinkedHashMap::new,
                        Collectors.mapping(MethodOccurrence::method,
                                Collectors.collectingAndThen(Collectors.toList(),
                                        methods -> methods.stream().distinct().toList()))));
    }

    private List<OperationRelation> relations(List<Integer> operationIds, List<Integer> discoveredIds) {
        Map<Integer, OperationRelation> relations = new LinkedHashMap<>();
        if (!operationIds.isEmpty()) {
            operationRelationRepository.findAllByRelatedOperationIdInAndEntityTypeOperationRelation(operationIds,
                    OPERATION).forEach(relation -> relations.putIfAbsent(relation.getId(), relation));
            operationRelationRepository.findAllByOperationIdInAndEntityTypeOperation(operationIds, OPERATION)
                    .forEach(relation -> relations.putIfAbsent(relation.getId(), relation));
        }
        if (!discoveredIds.isEmpty()) {
            operationRelationRepository.findAllByRelatedOperationIdInAndEntityTypeOperationRelation(discoveredIds,
                    DISCOVERED_OPERATION).forEach(relation -> relations.putIfAbsent(relation.getId(), relation));
            operationRelationRepository.findAllByOperationIdInAndEntityTypeOperation(discoveredIds,
                    DISCOVERED_OPERATION).forEach(relation -> relations.putIfAbsent(relation.getId(), relation));
        }
        return new ArrayList<>(relations.values());
    }

    private static boolean belongsToProduct(MethodRef method, List<Integer> operationIds,
                                            List<Integer> discoveredIds) {
        if (method.id() == null) {
            return false;
        }
        if (DISCOVERED_OPERATION.equals(method.entityType())) {
            return discoveredIds.contains(method.id());
        }
        return OPERATION.equals(method.entityType()) && operationIds.contains(method.id());
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

    private static ProductE2eCardDTO card(E2e e2e) {
        return new ProductE2eCardDTO(e2e.getId(), e2e.getCode(), e2e.getName(), e2e.getBiStepCode(), e2e.getSource());
    }

    private record MethodRef(String entityType, Integer id) {
    }

    private record MethodOccurrence(Integer e2eId, Integer order, Integer relationId, MethodRef method) {
    }
}
