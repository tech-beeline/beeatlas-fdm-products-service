/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.client.CapabilityClient;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Sla;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredConnectedOperationDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredOperationByIdsDTO;
import ru.beeline.fdmproducts.exception.ExternalServiceException;
import ru.beeline.fdmproducts.mapper.DiscoveredOperationByIdsMapper;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscoveredOperationByIdsService {

    private static final String MISSING_BODY = "Отсутствует тело запроса";
    private static final String EMPTY_IDS = "Список идентификаторов методов не может быть пустым";
    private static final String INVALID_ID = "Идентификатор метода не может быть пустым";
    private static final String CAPABILITY_UNAVAILABLE = "Сервис capability недоступен";

    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final OperationRepository operationRepository;
    private final SlaRepository slaRepository;
    private final InterfaceRepository interfaceRepository;
    private final ContainerRepository containerRepository;
    private final CapabilityClient capabilityClient;
    private final DiscoveredOperationByIdsMapper mapper;

    @Transactional(readOnly = true)
    public List<DiscoveredOperationByIdsDTO> getByIds(JsonNode body) {
        List<Integer> uniqueIds = uniqueIds(parseIds(body));
        log.info("Чтение discovered_operation по id: уникальных={}", uniqueIds.size());
        List<DiscoveredOperation> found = discoveredOperationRepository.findAllByIdInWithInterface(uniqueIds);
        Map<Integer, DiscoveredOperation> byId = found.stream()
                .collect(Collectors.toMap(DiscoveredOperation::getId, Function.identity(), (left, right) -> left));

        Map<Integer, DiscoveredConnectedOperationDTO> connectedById = loadConnectedOperations(found);
        log.info("Найдено discovered_operation={}, канонических operation={}, неизвестных id={}",
                found.size(), connectedById.size(), uniqueIds.size() - found.size());

        List<DiscoveredOperationByIdsDTO> result = new ArrayList<>();
        for (Integer id : uniqueIds) {
            DiscoveredOperation discoveredOperation = byId.get(id);
            if (discoveredOperation == null) {
                continue;
            }
            DiscoveredConnectedOperationDTO operation = discoveredOperation.getConnectionOperationId() == null
                    ? null
                    : connectedById.get(discoveredOperation.getConnectionOperationId());
            result.add(mapper.toDto(discoveredOperation, discoveredOperation.getDiscoveredInterface(), operation));
        }
        log.info("Чтение discovered_operation по id завершено: элементов в ответе={}", result.size());
        return result;
    }

    private List<Integer> parseIds(JsonNode body) {
        if (body == null || body.isNull() || body.isMissingNode() || !body.isArray()) {
            throw new IllegalArgumentException(MISSING_BODY);
        }
        if (body.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_IDS);
        }
        List<Integer> ids = new ArrayList<>(body.size());
        for (JsonNode item : body) {
            if (item == null || item.isNull() || !item.isIntegralNumber() || !item.canConvertToInt()) {
                throw new IllegalArgumentException(INVALID_ID);
            }
            ids.add(item.intValue());
        }
        return ids;
    }

    private List<Integer> uniqueIds(List<Integer> ids) {
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private Map<Integer, DiscoveredConnectedOperationDTO> loadConnectedOperations(List<DiscoveredOperation> found) {
        Set<Integer> operationIds = found.stream()
                .map(DiscoveredOperation::getConnectionOperationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (operationIds.isEmpty()) {
            return Map.of();
        }
        List<Operation> operations = operationRepository.findAllByIdIn(new ArrayList<>(operationIds));
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

        Map<Integer, TcDTO> techCapabilities = loadTechCapabilities(operations);

        Map<Integer, DiscoveredConnectedOperationDTO> result = new LinkedHashMap<>();
        for (Operation operation : operations) {
            Interface iface = operation.getInterfaceId() == null
                    ? null
                    : interfacesById.get(operation.getInterfaceId());
            ContainerProduct container = iface == null || iface.getContainerId() == null
                    ? null
                    : containersById.get(iface.getContainerId());
            TcDTO techCapability = operation.getTcId() == null
                    ? null
                    : techCapabilities.get(operation.getTcId());
            result.put(operation.getId(), mapper.toConnectedOperationDto(
                    operation,
                    techCapability,
                    slaByOperationId.get(operation.getId()),
                    iface,
                    container));
        }
        return result;
    }

    private Map<Integer, TcDTO> loadTechCapabilities(List<Operation> operations) {
        List<Integer> tcIds = operations.stream()
                .map(Operation::getTcId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (tcIds.isEmpty()) {
            log.info("ТС не запрашиваются: нет tc_id у отобранных operation");
            return Map.of();
        }
        log.info("Запрос ТС в capability: count={}, tcIds={}", tcIds.size(), tcIds);
        List<TcDTO> tcs;
        try {
            tcs = capabilityClient.getTcs(tcIds);
        } catch (RuntimeException e) {
            log.error("Ошибка при запросе ТС из capability: {}", e.getMessage(), e);
            throw new ExternalServiceException(HttpStatus.BAD_GATEWAY, CAPABILITY_UNAVAILABLE);
        }
        if (tcs == null || tcs.isEmpty()) {
            log.info("Capability вернул пустое тело: ТС не сопоставляются");
            return Map.of();
        }
        Map<Integer, TcDTO> byId = tcs.stream()
                .filter(tc -> tc != null && tc.getId() != null)
                .collect(Collectors.toMap(TcDTO::getId, Function.identity(), (left, right) -> left));
        log.info("ТС получены из capability: запрошено={}, получено={}", tcIds.size(), byId.size());
        return byId;
    }
}
