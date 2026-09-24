/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.repository.*;

import java.util.List;
import java.util.Optional;

@Transactional
@Service
@Slf4j
public class ComparisonOperationsService {

    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final OperationRepository operationRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final ArchOperationMatchingService archOperationMatchingService;

    public ComparisonOperationsService(DiscoveredOperationRepository discoveredOperationRepository,
                                       OperationRepository operationRepository,
                                       DiscoveredInterfaceRepository discoveredInterfaceRepository,
                                       ArchOperationMatchingService archOperationMatchingService) {
        this.discoveredOperationRepository = discoveredOperationRepository;
        this.operationRepository = operationRepository;
        this.discoveredInterfaceRepository = discoveredInterfaceRepository;
        this.archOperationMatchingService = archOperationMatchingService;
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
        Product product = discoveredOperation.getDiscoveredInterface().getProduct();
        // Область поиска и правило сравнения — общие с POST /api/v1/operation/search-matched,
        // чтобы предложенное UI сопоставление совпадало с тем, что проставит автомат.
        List<Integer> interfaceIds = archOperationMatchingService.resolveInterfaceIds(
                product == null ? null : product.getAlias());
        log.info("[INTERFACE] Область поиска: интерфейсов={}. ID: {}", interfaceIds.size(), interfaceIds);
        if (interfaceIds.isEmpty()) {
            return;
        }

        // Попытки 2 и 3 добавляют к имени контекст — им нет аналога в search-matched: у кандидата из
        // UI контекста discovered-операции нет.
        Optional<ArchOperationProjection> operation = attempt(1, discoveredOperation.getName(),
                discoveredOperation.getType(), interfaceIds);
        if (operation.isEmpty()) {
            operation = attempt(2, discoveredOperation.getContext() + discoveredOperation.getName(),
                    discoveredOperation.getType(), interfaceIds);
        }
        if (operation.isEmpty()) {
            operation = attempt(3, discoveredOperation.getDiscoveredInterface().getContext() + discoveredOperation.getName(),
                    discoveredOperation.getType(), interfaceIds);
        }
        if (operation.isEmpty()) {
            return;
        }

        Integer matchedOperationId = operation.get().getOpId();
        Integer matchedInterfaceId = operation.get().getInterfaceId();
        log.info("[ОБНОВЛЕНИЕ] Установка connectionOperationId={} для discoveredOperationId={}",
                matchedOperationId, discoveredOperation.getId());
        discoveredOperation.setConnectionOperationId(matchedOperationId);
        discoveredOperationRepository.save(discoveredOperation);

        List<Operation> operationList = operationRepository.findAllByInterfaceIdAndDeletedDateIsNull(matchedInterfaceId);
        log.info("[СПИСОК_OPERATION] Найдено операций для interfaceId={}: {}. ID: {}",
                discoveredOperation.getInterfaceId(),
                operationList.size(),
                operationList.stream().map(Operation::getId).toList());
        List<DiscoveredOperation> discoveredOperationList = discoveredOperationRepository
                .findAllByInterfaceIdAndDeletedDateIsNull(discoveredOperation.getInterfaceId());
        log.info("[СПИСОК_DISCOVERED_OPERATION] Найдено записей: {}. ID: {}",
                discoveredOperationList.size(),
                discoveredOperationList.stream().map(DiscoveredOperation::getId).toList());
        //все ли в discoveredOperationList элементы connectionOperationId == operation
        List<Integer> operationIds = operationList.stream().map(Operation::getId).toList();
        if (discoveredOperationList.size() == discoveredOperationList.stream()
                .filter(ds -> operationIds.contains(ds.getConnectionOperationId()))
                .count()) {
            discoveredOperation.getDiscoveredInterface().setConnectionInterfaceId(matchedInterfaceId);
            discoveredInterfaceRepository.save(discoveredOperation.getDiscoveredInterface());
        }
    }

    private Optional<ArchOperationProjection> attempt(int number, String name, String type, List<Integer> interfaceIds) {
        log.info("[ПОИСК_OPERATION] Попытка {}. Параметры: name='{}', type='{}', interfaceIds={}",
                number, name, type, interfaceIds);
        Optional<ArchOperationProjection> operation = archOperationMatchingService.findFirstMatch(name, type, interfaceIds);
        log.info("[РЕЗУЛЬТАТ] Попытка {}. Операция найдена: {}", number, operation.isPresent());
        return operation;
    }
}
