/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.repository.*;
import ru.beeline.fdmproducts.utils.OperationPathMatcher;

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
    private final ProductBranchService productBranchService;

    public ArchContainerRelationsService(DiscoveredOperationRepository discoveredOperationRepository,
                                         DiscoveredInterfaceRepository discoveredInterfaceRepository,
                                         OperationRepository operationRepository,
                                         InterfaceRepository interfaceRepository,
                                         ContainerRepository containerRepository,
                                         ProductBranchService productBranchService) {
        this.discoveredOperationRepository = discoveredOperationRepository;
        this.discoveredInterfaceRepository = discoveredInterfaceRepository;
        this.operationRepository = operationRepository;
        this.interfaceRepository = interfaceRepository;
        this.containerRepository = containerRepository;
        this.productBranchService = productBranchService;
    }

    public void processContainerDelete(Integer entityId) {
        log.info("[СТАРТ] Начало обработки processContainerDelete entityId={}", entityId);
        if (!productBranchService.isContainerInDefaultBranch(entityId)) {
            log.info("Container entityId={} не из ветки main, связи discovered не сбрасываются", entityId);
            return;
        }
        discoveredInterfaceRepository.clearConnectionInterfaceIdByEntityId(entityId);
        discoveredOperationRepository.clearConnectionOperationIdByEntityId(entityId);
    }

    public void processInterfaceDelete(int entityId) {
        log.info("[СТАРТ] Начало обработки processInterfaceDelete entityId={}", entityId);
        if (!productBranchService.isInterfaceInDefaultBranch(entityId)) {
            log.info("Interface entityId={} не из ветки main, связи discovered не сбрасываются", entityId);
            return;
        }
        discoveredInterfaceRepository.clearConnectionInterfaceIdByInterfaceId(entityId);
        discoveredOperationRepository.clearConnectionOperationIdByInterfaceId(entityId);
    }

    public void processOperationDelete(int entityId) {
        log.info("[СТАРТ] Начало обработки processOperationDelete entityId={}", entityId);
        if (!productBranchService.isOperationInDefaultBranch(entityId)) {
            log.info("Operation entityId={} не из ветки main, связи discovered не сбрасываются", entityId);
            return;
        }
        discoveredInterfaceRepository.clearConnectionInterfaceIdByOperationId(entityId);
        discoveredOperationRepository.clearConnectionOperationIdByOperationId(entityId);
    }

    public void processOperationComparison(int entityId) {
        log.info("[СТАРТ] Начало обработки processOperationComparison entityId={}", entityId);
        if (!productBranchService.isOperationInDefaultBranch(entityId)) {
            log.info("Operation entityId={} не из ветки main, сопоставление не выполняется", entityId);
            return;
        }
        Optional<Operation> operation = operationRepository.findById(entityId);
        if (operation.isPresent()) {
            Interface interfaceEntity = interfaceRepository.findById(operation.get().getInterfaceId()).get();
            ContainerProduct containerProduct = containerRepository.findById(interfaceEntity.getContainerId()).get();
            List<DiscoveredInterface> discoveredInterfaces = discoveredInterfaceRepository.findAllByProductIdAndArchInterfaceIdAndConnectionInterfaceIdIsNull(
                    containerProduct.getProductBranch().getProduct().getId(), interfaceEntity.getId());
            log.info("[ШАГ ] discoveredInterfaces size is {}", discoveredInterfaces.size());
            discoveredInterfaces.forEach(discoveredInterface -> {
                log.info("[ШАГ ] discoveredInterface is {}", discoveredInterface.getName());
                AtomicReference<Integer> discoveredOperationCounter = new AtomicReference<>(0);
                discoveredInterface.getOperations().forEach(discoveredOperation -> {
                    if (matchesArchOperation(discoveredOperation, discoveredInterface, operation.get())) {
                        discoveredOperation.setConnectionOperationId(entityId);
                        log.info("Сопоставлено с arch-операцией name={}, type={} (discoveredOperationId={})",
                                operation.get().getName(), operation.get().getType(), discoveredOperation.getId());
                    }
                    if (discoveredOperation.getConnectionOperationId() == null) {
                        discoveredOperationCounter.getAndSet(discoveredOperationCounter.get() + 1);
                    }
                });
                log.info("[ШАГ ] discoveredOperationCounter.get() {}", discoveredOperationCounter.get());
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
                        log.info("[ШАГ ] sameInterface is {}", sameInterface);
                        Integer connectionInterfaceId = operationList.get(0).getInterfaceId();
                        discoveredInterface.setConnectionInterfaceId(connectionInterfaceId);
                        discoveredInterfaceRepository.save(discoveredInterface);
                        log.info("Связан интерфейс: connectionInterfaceId={} (discoveredInterfaceId={})",
                                connectionInterfaceId, discoveredInterface.getId());
                    }
                }
                discoveredOperationRepository.saveAll(discoveredInterface.getOperations());
            });
        }
    }


    private boolean matchesArchOperation(DiscoveredOperation discoveredOperation,
                                          DiscoveredInterface discoveredInterface, Operation operation) {
        if (!OperationPathMatcher.typeMatches(operation.getType(), discoveredOperation.getType())) {
            return false;
        }
        String name = discoveredOperation.getName();
        return OperationPathMatcher.matches(operation.getName(), name)
                || OperationPathMatcher.matches(operation.getName(),
                        concatContext(discoveredOperation.getContext(), name))
                || OperationPathMatcher.matches(operation.getName(),
                        concatContext(discoveredInterface.getContext(), name));
    }

    private String concatContext(String context, String name) {
        if (context == null)
            context = "";
        if (context.endsWith("/") && name.startsWith("/")) {
            name = name.substring(1, name.length());
        }
        if (context.endsWith("/") || name.startsWith("/")) {
            return context + name;
        } else {
            return context + "/" + name;
        }
    }
}
