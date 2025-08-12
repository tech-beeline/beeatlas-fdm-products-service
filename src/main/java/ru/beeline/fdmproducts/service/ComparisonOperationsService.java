package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class ComparisonOperationsService {
    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final InterfaceRepository interfaceRepository;
    private final ContainerRepository containerRepository;
    private final OperationRepository operationRepository;

    public ComparisonOperationsService(DiscoveredOperationRepository discoveredOperationRepository,
                                       InterfaceRepository interfaceRepository,
                                       ContainerRepository containerRepository,
                                       OperationRepository operationRepository) {
        this.discoveredOperationRepository = discoveredOperationRepository;
        this.interfaceRepository = interfaceRepository;
        this.containerRepository = containerRepository;
        this.operationRepository = operationRepository;
    }

    public void process(Integer entityId) {
        log.info("start process");

        DiscoveredOperation discoveredOperation = discoveredOperationRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Отсутствует DiscoveredOperation с id=" + entityId));
        log.info("discoveredOperation id=" + discoveredOperation.getId());
        List<ContainerProduct> containerProductList = containerRepository.findAllByProductId(discoveredOperation.getDiscoveredInterface()
                                                                                                     .getProduct()
                                                                                                     .getId());
        log.info("containerProductList ids=" + containerProductList.stream()
                .map(ContainerProduct::getId)
                .collect(Collectors.toList()));
        if (!containerProductList.isEmpty()) {
            List<Interface> interfaceProductList = interfaceRepository.findAllByContainerIdIn(containerProductList.stream()
                                                                                                      .map(ContainerProduct::getId)
                                                                                                      .collect(
                                                                                                              Collectors.toList()));
            log.info("interfaceProductList ids=" + interfaceProductList.stream()
                    .map(Interface::getId)
                    .collect(Collectors.toList()));
            Optional<Operation> operation = operationRepository.findByNameAndTypeAndInterfaceIdIn(discoveredOperation.getName(),
                                                                                                  discoveredOperation.getType(),
                                                                                                  interfaceProductList.stream()
                                                                                                          .map(Interface::getId)
                                                                                                          .collect(
                                                                                                                  Collectors.toList()));
            log.info("Operation attempt1 isEmpty=" + operation.isEmpty());
            if (operation.isEmpty()) {
                operation = operationRepository.findByNameAndTypeAndInterfaceIdIn(discoveredOperation.getContext() + discoveredOperation.getName(),
                                                                                  discoveredOperation.getType(),
                                                                                  interfaceProductList.stream()
                                                                                          .map(Interface::getId)
                                                                                          .collect(Collectors.toList()));
                log.info("Operation attempt2 isEmpty=" + operation.isEmpty());
            }
            if (operation.isEmpty()) {
                operation = operationRepository.findByNameAndTypeAndInterfaceIdIn(discoveredOperation.getDiscoveredInterface()
                                                                                          .getContext() + discoveredOperation.getName(),
                                                                                  discoveredOperation.getType(),
                                                                                  interfaceProductList.stream()
                                                                                          .map(Interface::getId)
                                                                                          .collect(Collectors.toList()));
                log.info("Operation attempt3 isEmpty=" + operation.isEmpty());
            }
            if (operation.isPresent()) {
                log.info("Operation Id=" + operation.get().getId());
                discoveredOperation.setConnectionOperationId(operation.get().getId());
                discoveredOperationRepository.save(discoveredOperation);
                List<Operation> operationList = operationRepository.findAllByInterfaceId(discoveredOperation.getInterfaceId());
                log.info("operationList ids=" + operationList.stream()
                        .map(Operation::getId)
                        .collect(Collectors.toList()));
                List<DiscoveredOperation> discoveredOperationList = discoveredOperationRepository.findAllByConnectionOperationIdIn(
                        operationList.stream().map(Operation::getId).collect(Collectors.toList()));
                log.info("discoveredOperationListIds=" + discoveredOperationList.stream()
                        .map(DiscoveredOperation::getId)
                        .collect(Collectors.toList()));

                List<Integer> discoveredOperationIdListFiltered = discoveredOperationList.stream()
                        .filter(op -> op.getInterfaceId().equals(discoveredOperation.getInterfaceId()))
                        .map(DiscoveredOperation::getId)
                        .collect(Collectors.toList());
                log.info("discoveredOperationListIds=" + discoveredOperationList.stream()
                        .map(DiscoveredOperation::getId)
                        .collect(Collectors.toList()));

                if (discoveredOperationList.size() == discoveredOperationIdListFiltered.size()) {
                    discoveredOperationIdListFiltered.add(-1);
                    if (discoveredOperationRepository.getRows(discoveredOperation.getInterfaceId(),
                                                              discoveredOperationIdListFiltered).isEmpty()) {
                        log.info("setConnectionInterfaceId");
                        discoveredOperation.getDiscoveredInterface()
                                .setConnectionInterfaceId(operation.get().getInterfaceId());
                        discoveredOperationRepository.save(discoveredOperation);
                    }
                }

            }
        }
    }
}
