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
        log.info("[СТАРТ] Начало обработки entityId={}", entityId);

        DiscoveredOperation discoveredOperation = discoveredOperationRepository.findById(entityId)
                .orElseThrow(() -> new IllegalArgumentException("Отсутствует DiscoveredOperation с id=" + entityId));
        log.info("[DISCOVERED_OPERATION] id={}, name='{}', type='{}', interfaceId={}, context='{}'",
                discoveredOperation.getId(), discoveredOperation.getName(), discoveredOperation.getType(),
                discoveredOperation.getInterfaceId(), discoveredOperation.getContext());
        List<ContainerProduct> containerProductList = containerRepository.findAllByProductId(discoveredOperation.getDiscoveredInterface()
                .getProduct().getId());
        log.info("[CONTAINER_PRODUCT] Найдено записей: {}. ID: {}",
                containerProductList.size(), containerProductList.stream().map(ContainerProduct::getId).collect(Collectors.toList()));
        if (!containerProductList.isEmpty()) {
            List<Interface> interfaceProductList = interfaceRepository.findAllByContainerIdIn(containerProductList.stream()
                    .map(ContainerProduct::getId)
                    .collect(Collectors.toList()));
            log.info("[INTERFACE] Найдено интерфейсов: {}. ID: {}",
                    interfaceProductList.size(), interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
            Optional<Operation> operation = operationRepository.findByNameAndTypeAndInterfaceIdIn(discoveredOperation.getName(),
                    discoveredOperation.getType(), interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
            log.info("[ПОИСК_OPERATION] Попытка 1. Параметры: name='{}', type='{}', interfaceIds={}",
                    discoveredOperation.getName(), discoveredOperation.getType(),
                    interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
            log.info("[РЕЗУЛЬТАТ] Попытка 1. Операция найдена: {}", !operation.isEmpty());
            if (operation.isEmpty()) {
                operation = operationRepository.findByNameAndTypeAndInterfaceIdIn(discoveredOperation.getContext() + discoveredOperation.getName(),
                        discoveredOperation.getType(), interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
                log.info("[ПОИСК_OPERATION] Попытка 2. Параметры: name='{}', type='{}', interfaceIds={}",
                        discoveredOperation.getContext() + discoveredOperation.getName(), discoveredOperation.getType(),
                        interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
                log.info("[РЕЗУЛЬТАТ] Попытка 2. Операция найдена: {}", !operation.isEmpty());
            }
            if (operation.isEmpty()) {
                operation = operationRepository.findByNameAndTypeAndInterfaceIdIn(discoveredOperation.getDiscoveredInterface()
                                .getContext() + discoveredOperation.getName(),
                        discoveredOperation.getType(), interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
                log.info("[ПОИСК_OPERATION] Попытка 3. Параметры: name='{}', type='{}', interfaceIds={}",
                        discoveredOperation.getDiscoveredInterface().getContext() + discoveredOperation.getName(),
                        discoveredOperation.getType(), interfaceProductList.stream().map(Interface::getId).collect(Collectors.toList()));
                log.info("[РЕЗУЛЬТАТ] Попытка 3. Операция найдена: {}", !operation.isEmpty());
            }
            if (operation.isPresent()) {
                log.info("[ОБНОВЛЕНИЕ] Установка connectionOperationId={} для discoveredOperationId={}",
                        operation.get().getId(), discoveredOperation.getId());
                discoveredOperation.setConnectionOperationId(operation.get().getId());
                discoveredOperationRepository.save(discoveredOperation);

                List<Operation> operationList = operationRepository.findAllByInterfaceId(discoveredOperation.getInterfaceId());
                log.info("[СПИСОК_OPERATION] Найдено операций для interfaceId={}: {}. ID: {}",
                        discoveredOperation.getInterfaceId(), operationList.size(),
                        operationList.stream().map(Operation::getId).collect(Collectors.toList()));
                List<DiscoveredOperation> discoveredOperationList = discoveredOperationRepository.findAllByConnectionOperationIdIn(
                        operationList.stream().map(Operation::getId).collect(Collectors.toList()));
                log.info("[СПИСОК_DISCOVERED_OPERATION] Найдено записей: {}. ID: {}",
                        discoveredOperationList.size(),
                        discoveredOperationList.stream().map(DiscoveredOperation::getId).collect(Collectors.toList()));
                List<Integer> discoveredOperationIdListFiltered = discoveredOperationList.stream()
                        .filter(op -> op.getInterfaceId().equals(discoveredOperation.getInterfaceId()))
                        .map(DiscoveredOperation::getId)
                        .collect(Collectors.toList());
                log.info("[ФИЛЬТРАЦИЯ] После фильтрации по interfaceId={} остались ID: {}",
                        discoveredOperation.getInterfaceId(), discoveredOperationIdListFiltered);
                if (discoveredOperationList.size() == discoveredOperationIdListFiltered.size()) {
                    discoveredOperationIdListFiltered.add(-1);
                    log.info("[ПРОВЕРКА_УНИКАЛЬНОСТИ] Проверка записей для interfaceId={} с исключёнными ID: {}",
                            discoveredOperation.getInterfaceId(), discoveredOperationIdListFiltered);
                    if (discoveredOperationRepository.getRows(discoveredOperation.getInterfaceId(),
                            discoveredOperationIdListFiltered).isEmpty()) {
                        log.info("[ПРИВЯЗКА_INTERFACE] Установка connectionInterfaceId={} для discoveredInterfaceId={}",
                                operation.get().getInterfaceId(), discoveredOperation.getDiscoveredInterface().getId());
                        discoveredOperation.getDiscoveredInterface().setConnectionInterfaceId(operation.get().getInterfaceId());
                        discoveredOperationRepository.save(discoveredOperation);
                    } else {
                        log.info("[ПРОПУСК] Найдены записи для interfaceId={}. Привязка не выполнена.",
                                discoveredOperation.getInterfaceId());
                    }
                } else {
                    log.info("[ПРОПУСК] Не все DiscoveredOperations обработаны для interfaceId={}. Привязка не выполнена.",
                            discoveredOperation.getInterfaceId());
                }
            }
        }
    }
}
