/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Sla;
import ru.beeline.fdmproducts.dto.SlaV2DTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2ConnectedContainerDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2ConnectedInterfaceDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2ConnectedOperationDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2ConnectedOperationSlaDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2DiscoveredOperationCatalogItemDTO;
import ru.beeline.fdmproducts.dto.search.projection.DiscoveredOperationProjection;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Справочник discoveredOperations дерева e2e (GET /api/v2/e2e/{code} и GET /api/v3/e2e/{code}): карточка
 * обнаруженного метода и вложенная операция архитектуры по connection_operation_id со sla, interface и container.
 * product.operation, sla, interface и containers_product читаются по одному запросу на непустой набор id;
 * удалённые строки не отбрасываются. Один connection_operation_id — один и тот же вложенный объект.
 */
@Component
@RequiredArgsConstructor
public class E2eDiscoveredOperationCatalog {

    private final OperationRepository operationRepository;
    private final SlaRepository slaRepository;
    private final InterfaceRepository interfaceRepository;
    private final ContainerRepository containerRepository;

    /** Карточки в порядке orderedIds; id без строки discovered_operation пропускаются. */
    public List<E2eV2DiscoveredOperationCatalogItemDTO> build(Collection<Integer> orderedIds,
                                                              List<DiscoveredOperationProjection> projections) {
        List<DiscoveredOperationProjection> rows = projections.stream().filter(Objects::nonNull).toList();
        Set<Integer> connectionOperationIds = rows.stream()
                .map(DiscoveredOperationProjection::getConnectionOperationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Integer, E2eV2ConnectedOperationDTO> connectedById = loadConnectedOperations(connectionOperationIds);

        Map<Integer, E2eV2DiscoveredOperationCatalogItemDTO> catalogById = rows.stream()
                .collect(Collectors.toMap(
                        DiscoveredOperationProjection::getOpId,
                        projection -> mapItem(projection, connectedById),
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new));
        return orderedIds.stream()
                .map(catalogById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<Integer, E2eV2ConnectedOperationDTO> loadConnectedOperations(Set<Integer> connectionOperationIds) {
        if (connectionOperationIds.isEmpty()) {
            return Map.of();
        }
        List<Operation> operations = operationRepository.findAllByIdIn(new ArrayList<>(connectionOperationIds));
        if (operations.isEmpty()) {
            return Map.of();
        }

        List<Integer> foundOperationIds = operations.stream().map(Operation::getId).toList();
        Map<Integer, Sla> slaByOperationId = slaRepository.findAllByOperationIdIn(foundOperationIds).stream()
                .collect(Collectors.toMap(
                        Sla::getOperationId,
                        sla -> sla,
                        (left, right) -> left.getId() <= right.getId() ? left : right));

        List<Integer> interfaceIds = operations.stream()
                .map(Operation::getInterfaceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, Interface> interfacesById = new HashMap<>();
        if (!interfaceIds.isEmpty()) {
            for (Interface row : interfaceRepository.findAllByIdIn(interfaceIds)) {
                interfacesById.put(row.getId(), row);
            }
        }

        List<Integer> containerIds = interfacesById.values().stream()
                .map(Interface::getContainerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, ContainerProduct> containersById = new HashMap<>();
        if (!containerIds.isEmpty()) {
            for (ContainerProduct row : containerRepository.findAllByIdIn(containerIds)) {
                containersById.put(row.getId(), row);
            }
        }

        Map<Integer, E2eV2ConnectedOperationDTO> result = new LinkedHashMap<>();
        for (Operation operation : operations) {
            result.put(operation.getId(), mapConnectedOperation(operation, slaByOperationId, interfacesById, containersById));
        }
        return result;
    }

    private E2eV2ConnectedOperationDTO mapConnectedOperation(Operation operation,
                                                             Map<Integer, Sla> slaByOperationId,
                                                             Map<Integer, Interface> interfacesById,
                                                             Map<Integer, ContainerProduct> containersById) {
        E2eV2ConnectedInterfaceDTO connectedInterface = null;
        if (operation.getInterfaceId() != null) {
            Interface iface = interfacesById.get(operation.getInterfaceId());
            if (iface != null) {
                E2eV2ConnectedContainerDTO container = null;
                if (iface.getContainerId() != null) {
                    ContainerProduct containerProduct = containersById.get(iface.getContainerId());
                    if (containerProduct != null) {
                        container = E2eV2ConnectedContainerDTO.builder()
                                .id(containerProduct.getId())
                                .code(containerProduct.getCode())
                                .build();
                    }
                }
                connectedInterface = E2eV2ConnectedInterfaceDTO.builder()
                        .id(iface.getId())
                        .code(iface.getCode())
                        .container(container)
                        .build();
            }
        }
        return E2eV2ConnectedOperationDTO.builder()
                .id(operation.getId())
                .name(operation.getName())
                .type(operation.getType())
                .tcId(operation.getTcId())
                .sla(toConnectedOperationSla(slaByOperationId.get(operation.getId())))
                .connectedInterface(connectedInterface)
                .build();
    }

    private static E2eV2ConnectedOperationSlaDTO toConnectedOperationSla(Sla sla) {
        if (sla == null) {
            return null;
        }
        return E2eV2ConnectedOperationSlaDTO.builder()
                .id(sla.getId())
                .operationId(sla.getOperationId())
                .rps(sla.getRps())
                .latency(sla.getLatency())
                .errorRate(sla.getErrorRate())
                .build();
    }

    private static E2eV2DiscoveredOperationCatalogItemDTO mapItem(DiscoveredOperationProjection projection,
                                                                  Map<Integer, E2eV2ConnectedOperationDTO> connectedById) {
        boolean hasSla = projection.getRps() != null || projection.getLatency() != null || projection.getErrorRate() != null;
        E2eV2ConnectedOperationDTO connected = projection.getConnectionOperationId() == null
                ? null
                : connectedById.get(projection.getConnectionOperationId());
        return E2eV2DiscoveredOperationCatalogItemDTO.builder()
                .id(projection.getOpId())
                .name(projection.getOpName())
                .type(projection.getOpType())
                .interfaceCode(projection.getInterfaceCode())
                .productAlias(projection.getProductAlias())
                .source(projection.getSource())
                .sla(hasSla ? SlaV2DTO.builder()
                        .rps(projection.getRps())
                        .latency(projection.getLatency())
                        .errorRate(projection.getErrorRate())
                        .build() : null)
                .operation(connected)
                .build();
    }
}
