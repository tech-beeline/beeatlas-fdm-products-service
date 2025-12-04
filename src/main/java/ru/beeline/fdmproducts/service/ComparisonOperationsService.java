package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.repository.*;

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
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;

    public ComparisonOperationsService(DiscoveredOperationRepository discoveredOperationRepository,
                                       InterfaceRepository interfaceRepository,
                                       ContainerRepository containerRepository,
                                       OperationRepository operationRepository,
                                       DiscoveredInterfaceRepository discoveredInterfaceRepository) {
        this.discoveredOperationRepository = discoveredOperationRepository;
        this.interfaceRepository = interfaceRepository;
        this.containerRepository = containerRepository;
        this.operationRepository = operationRepository;
        this.discoveredInterfaceRepository = discoveredInterfaceRepository;
    }

    public void process(Integer id) {
        log.info("[СТАРТ] Начало обработки id={}", id);

        DiscoveredOperation discoveredOperation = discoveredOperationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Отсутствует DiscoveredOperation с id=" + id));
        log.info("[DISCOVERED_OPERATION] id={}, name='{}', type='{}', interfaceId={}, context='{}'",
                discoveredOperation.getId(),
                discoveredOperation.getName(),
                discoveredOperation.getType(),
                discoveredOperation.getInterfaceId(),
                discoveredOperation.getContext());
        List<ContainerProduct> containerProductList = containerRepository.findAllByProductId(
                discoveredOperation.getDiscoveredInterface().getProduct().getId());
        log.info("[CONTAINER_PRODUCT] Найдено записей: {}. ID: {}",
                containerProductList.size(),
                containerProductList.stream().map(ContainerProduct::getId).collect(Collectors.toList()));
        if (!containerProductList.isEmpty()) {
            List<Interface> interfaceProductList = interfaceRepository.findAllByContainerIdIn(containerProductList.stream()
                    .map(ContainerProduct::getId)
                    .collect(
                            Collectors.toList()));
            log.info("[INTERFACE] Найдено интерфейсов: {}. ID: {}",
                    interfaceProductList.size(),
                    interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
            Optional<Operation> operation = operationRepository.findByNameAndTypeILikeNative(discoveredOperation.getName(),
                    discoveredOperation.getType(),
                    interfaceProductList.stream()
                            .map(Interface::getId)
                            .collect(Collectors.toList()));
            log.info("[ПОИСК_OPERATION] Попытка 1. Параметры: name='{}', type='{}', interfaceIds={}",
                    discoveredOperation.getName(),
                    discoveredOperation.getType(),
                    interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
            log.info("[РЕЗУЛЬТАТ] Попытка 1. Операция найдена: {}", !operation.isEmpty());
            if (operation.isEmpty()) {
                operation = operationRepository.findByNameAndTypeILikeNative(discoveredOperation.getContext() + discoveredOperation.getName(),
                        discoveredOperation.getType(),
                        interfaceProductList.stream()
                                .map(Interface::getId)
                                .collect(Collectors.toList()));
                log.info("[ПОИСК_OPERATION] Попытка 2. Параметры: name='{}', type='{}', interfaceIds={}",
                        discoveredOperation.getContext() + discoveredOperation.getName(),
                        discoveredOperation.getType(),
                        interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
                log.info("[РЕЗУЛЬТАТ] Попытка 2. Операция найдена: {}", !operation.isEmpty());
            }
            if (operation.isEmpty()) {
                operation = operationRepository.findByNameAndTypeILikeNative(discoveredOperation.getDiscoveredInterface()
                                .getContext() + discoveredOperation.getName(),
                        discoveredOperation.getType(),
                        interfaceProductList.stream()
                                .map(Interface::getId)
                                .collect(Collectors.toList()));
                log.info("[ПОИСК_OPERATION] Попытка 3. Параметры: name='{}', type='{}', interfaceIds={}",
                        discoveredOperation.getDiscoveredInterface().getContext() + discoveredOperation.getName(),
                        discoveredOperation.getType(),
                        interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
                log.info("[РЕЗУЛЬТАТ] Попытка 3. Операция найдена: {}", !operation.isEmpty());
            }
            if (operation.isPresent()) {
                log.info("[ОБНОВЛЕНИЕ] Установка connectionOperationId={} для discoveredOperationId={}",
                        operation.get().getId(),
                        discoveredOperation.getId());
                discoveredOperation.setConnectionOperationId(operation.get().getId());
                discoveredOperationRepository.save(discoveredOperation);

                List<Operation> operationList = operationRepository.findAllByInterfaceIdAndDeletedDateIsNull(operation.get()
                        .getInterfaceId());
                log.info("[СПИСОК_OPERATION] Найдено операций для interfaceId={}: {}. ID: {}",
                        discoveredOperation.getInterfaceId(),
                        operationList.size(),
                        operationList.stream().map(Operation::getId).collect(Collectors.toList()));
                List<DiscoveredOperation> discoveredOperationList = discoveredOperationRepository.findAllByInterfaceId(
                        discoveredOperation.getInterfaceId());
                log.info("[СПИСОК_DISCOVERED_OPERATION] Найдено записей: {}. ID: {}",
                        discoveredOperationList.size(),
                        discoveredOperationList.stream().map(DiscoveredOperation::getId).collect(Collectors.toList()));
                //все ли в discoveredOperationList элементы connectionOperationId == operation
                List<Integer> operationIds = operationList.stream().map(Operation::getId).toList();
                if (discoveredOperationList.size() == discoveredOperationList.stream()
                        .filter(ds -> operationIds.contains(ds.getConnectionOperationId()))
                        .count()) {
                    discoveredOperation.getDiscoveredInterface().setConnectionInterfaceId(operation.get()
                            .getInterfaceId());
                    discoveredInterfaceRepository.save(discoveredOperation.getDiscoveredInterface());
                }
            }
        }
    }
}
