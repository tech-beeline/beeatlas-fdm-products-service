/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.OperationTechCapabilityDTO;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.exception.ExternalServiceException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperationTechCapabilityResolver {

    private static final String CAPABILITY_UNAVAILABLE = "Сервис capability недоступен";

    private final CapabilityClient capabilityClient;

    public Map<Integer, TcDTO> load(Collection<Operation> operations) {
        List<Integer> tcIds = tcIdsOf(operations);
        if (tcIds.isEmpty()) {
            return Map.of();
        }
        try {
            return byId(capabilityClient.getTcs(tcIds));
        } catch (RuntimeException e) {
            log.error("Ошибка при запросе ТС из capability: {}", e.getMessage(), e);
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, CAPABILITY_UNAVAILABLE);
        }
    }

    public Map<Integer, TcDTO> loadOrEmpty(Collection<Operation> operations) {
        List<Integer> tcIds = tcIdsOf(operations);
        if (tcIds.isEmpty()) {
            return Map.of();
        }
        try {
            return byId(capabilityClient.getTcs(tcIds));
        } catch (RuntimeException e) {
            log.warn("Capability недоступен, ТС методов не заполняются: {}", e.getMessage());
            return Map.of();
        }
    }

    public static OperationTechCapabilityDTO of(Operation operation, Map<Integer, TcDTO> techCapabilities) {
        if (operation == null || operation.getTcId() == null) {
            return null;
        }
        TcDTO tc = techCapabilities.get(operation.getTcId());
        return tc == null ? null : new OperationTechCapabilityDTO(tc.getId(), tc.getCode(), tc.getName());
    }

    public static OperationTechCapabilityDTO of(DiscoveredOperation discoveredOperation, Operation connectedOperation,
                                                Map<Integer, TcDTO> techCapabilities) {
        OperationTechCapabilityDTO connected = of(connectedOperation, techCapabilities);
        if (connected != null) {
            return connected;
        }
        if (StringUtils.hasText(discoveredOperation.getTcCode())
                || StringUtils.hasText(discoveredOperation.getTcDescription())) {
            return new OperationTechCapabilityDTO(null, discoveredOperation.getTcCode(),
                    discoveredOperation.getTcDescription());
        }
        return null;
    }

    private static List<Integer> tcIdsOf(Collection<Operation> operations) {
        return operations.stream()
                .filter(Objects::nonNull)
                .map(Operation::getTcId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static Map<Integer, TcDTO> byId(List<TcDTO> techCapabilities) {
        if (techCapabilities == null) {
            return Map.of();
        }
        return techCapabilities.stream()
                .filter(tc -> tc != null && tc.getId() != null)
                .collect(Collectors.toMap(TcDTO::getId, Function.identity(), (left, right) -> left));
    }
}
