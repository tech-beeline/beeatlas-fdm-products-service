package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.repository.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class ArchContainerRelationsService {
    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final OperationRepository operationRepository;
    private final InterfaceRepository interfaceRepository;
    private final ContainerRepository containerRepository;

    public ArchContainerRelationsService(DiscoveredOperationRepository discoveredOperationRepository,
                                         DiscoveredInterfaceRepository discoveredInterfaceRepository,
                                         OperationRepository operationRepository,
                                         InterfaceRepository interfaceRepository,
                                         ContainerRepository containerRepository) {
        this.discoveredOperationRepository = discoveredOperationRepository;
        this.discoveredInterfaceRepository = discoveredInterfaceRepository;
        this.operationRepository = operationRepository;
        this.interfaceRepository = interfaceRepository;
        this.containerRepository = containerRepository;
    }

    public void processContainerDelete(Integer entityId) {
        log.info("[СТАРТ] Начало обработки processContainerDelete entityId={}", entityId);
        discoveredInterfaceRepository.clearConnectionInterfaceIdByEntityId(entityId);
        discoveredOperationRepository.clearConnectionOperationIdByEntityId(entityId);
    }

    public void processInterfaceDelete(int entityId) {
        log.info("[СТАРТ] Начало обработки processInterfaceDelete entityId={}", entityId);
        discoveredInterfaceRepository.clearConnectionInterfaceIdByInterfaceId(entityId);
        discoveredOperationRepository.clearConnectionOperationIdByInterfaceId(entityId);
    }

    public void processOperationDelete(int entityId) {
        log.info("[СТАРТ] Начало обработки processOperationDelete entityId={}", entityId);
        discoveredOperationRepository.clearConnectionOperationIdByOperationId(entityId);
        discoveredInterfaceRepository.clearConnectionInterfaceIdByOperationId(entityId);
    }

    public void processOperationComparison(int entityId, String name, String type) {
        log.info("[СТАРТ] Начало обработки processOperationComparison entityId={}", entityId);
        Optional<Operation> operation = operationRepository.findById(entityId);
        if (operation.isPresent()) {
            Interface interfaceEntity = interfaceRepository.findById(operation.get().getInterfaceId()).get();
            ContainerProduct containerProduct = containerRepository.findById(interfaceEntity.getContainerId()).get();
            List<DiscoveredInterface> discoveredInterfaces = discoveredInterfaceRepository.findAllByProductIdAndConnectionInterfaceIdIsNull(
                    containerProduct.getProductId());
            discoveredInterfaces.forEach(discoveredInterface -> {
                AtomicReference<Integer> discoveredOperationCounter = new AtomicReference<>(0);
                discoveredInterface.getOperations().forEach(discoveredOperation -> {
                    if (discoveredOperation.getName().equals(name) && discoveredOperation.getType().equals(type)) {
                        discoveredOperation.setConnectionOperationId(entityId);
                        log.debug("[ШАГ 1] Сопоставлено по name={}, type={} (operationId={})", name, type, discoveredOperation.getId());
                    } else {
                        if (concatContext(discoveredOperation.getContext(),
                                discoveredOperation.getName()).equals(name) && type.equals(discoveredOperation.getType())) {
                            discoveredOperation.setConnectionOperationId(entityId);
                            log.debug("[ШАГ 2] Сопоставлено по context+name='{}', type={} (operationId={})",
                                    concatContext(discoveredOperation.getContext(), discoveredOperation.getName()), type, discoveredOperation.getId());
                        } else {
                            if (concatContext(discoveredInterface.getContext(), discoveredInterface.getName()).equals(
                                    name) && type.equals(discoveredOperation.getType())) {
                                discoveredOperation.setConnectionOperationId(entityId);
                                log.debug("[ШАГ 3] Сопоставлено по parentContext+name='{}', type={} (operationId={})",
                                        concatContext(discoveredInterface.getContext(), discoveredInterface.getName()), type, discoveredOperation.getId());
                            }
                        }
                    }
                    if (discoveredOperation.getConnectionOperationId() == null) {
                        discoveredOperationCounter.getAndSet(discoveredOperationCounter.get() + 1);
                    }
                });
                if (discoveredOperationCounter.get() == 0) {
                    List<Operation> operationList = operationRepository.findAllByIdIn(discoveredInterface.getOperations()
                            .stream()
                            .map(DiscoveredOperation::getConnectionOperationId)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList()));
                    boolean sameInterface = operationList.stream()
                            .map(Operation::getInterfaceId)
                            .distinct()
                            .count() == 1;
                    if (sameInterface) {
                        Integer connectionInterfaceId = operationList.get(0).getInterfaceId();
                        discoveredInterface.setConnectionInterfaceId(connectionInterfaceId);
                        discoveredInterfaceRepository.save(discoveredInterface);
                        log.debug("Связан интерфейс: connectionInterfaceId={} (discoveredInterfaceId={})",
                                connectionInterfaceId, discoveredInterface.getId());
                    }
                }
                discoveredOperationRepository.saveAll(discoveredInterface.getOperations());
            });
        }
    }


    private String concatContext(String context, String name) {
        if (context == null)
            context = "";
        if (context.endsWith("/")) {
            return context + name;
        } else {
            return context + "/" + name;
        }
    }
}
