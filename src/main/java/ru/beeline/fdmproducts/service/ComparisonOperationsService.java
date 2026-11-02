/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.DiscoveredConnectionBranch;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.repository.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
        String productAlias = product == null ? null : product.getAlias();

        // Ветки main, develop и design ищутся независимо (SFDM-4319): совпадение одной ветки не
        // останавливает поиск по остальным и пишется только в атрибут своей ветки.
        Map<DiscoveredConnectionBranch, ArchOperationProjection> matches = new EnumMap<>(DiscoveredConnectionBranch.class);
        for (DiscoveredConnectionBranch branch : DiscoveredConnectionBranch.values()) {
            findMatch(discoveredOperation, productAlias, branch).ifPresent(match -> matches.put(branch, match));
        }
        if (matches.isEmpty()) {
            return;
        }

        matches.forEach((branch, match) -> {
            log.info("[ОБНОВЛЕНИЕ] Ветка {}. Установка connectionOperationId={} для discoveredOperationId={}",
                    branch.getBranchName(), match.getOpId(), discoveredOperation.getId());
            branch.setConnectionOperationId(discoveredOperation, match.getOpId());
        });
        discoveredOperationRepository.save(discoveredOperation);

        List<DiscoveredOperation> discoveredOperationList = discoveredOperationRepository
                .findAllByInterfaceIdAndDeletedDateIsNull(discoveredOperation.getInterfaceId());
        log.info("[СПИСОК_DISCOVERED_OPERATION] Найдено записей: {}. ID: {}",
                discoveredOperationList.size(),
                discoveredOperationList.stream().map(DiscoveredOperation::getId).toList());
        // Связь интерфейса пересчитывается только для веток, у которых в этом событии нашлось совпадение.
        matches.forEach((branch, match) -> connectInterface(discoveredOperation.getDiscoveredInterface(),
                discoveredOperationList, branch, match.getInterfaceId()));
    }

    private Optional<ArchOperationProjection> findMatch(DiscoveredOperation discoveredOperation, String productAlias,
                                                        DiscoveredConnectionBranch branch) {
        // Область поиска и правило сравнения — общие с POST /api/v1/operation/search-matched,
        // чтобы предложенное UI сопоставление совпадало с тем, что проставит автомат.
        List<Integer> interfaceIds = archOperationMatchingService.resolveInterfaceIds(productAlias, branch.getBranchName());
        log.info("[INTERFACE] Ветка {}. Область поиска: интерфейсов={}. ID: {}",
                branch.getBranchName(), interfaceIds.size(), interfaceIds);
        if (interfaceIds.isEmpty()) {
            return Optional.empty();
        }

        // Попытки 2 и 3 добавляют к имени контекст — им нет аналога в search-matched: у кандидата из
        // UI контекста discovered-операции нет.
        Optional<ArchOperationProjection> operation = attempt(branch, 1, discoveredOperation.getName(),
                discoveredOperation.getType(), interfaceIds);
        if (operation.isEmpty()) {
            operation = attempt(branch, 2, discoveredOperation.getContext() + discoveredOperation.getName(),
                    discoveredOperation.getType(), interfaceIds);
        }
        if (operation.isEmpty()) {
            operation = attempt(branch, 3, discoveredOperation.getDiscoveredInterface().getContext() + discoveredOperation.getName(),
                    discoveredOperation.getType(), interfaceIds);
        }
        return operation;
    }

    private void connectInterface(DiscoveredInterface discoveredInterface, List<DiscoveredOperation> discoveredOperationList,
                                  DiscoveredConnectionBranch branch, Integer matchedInterfaceId) {
        List<Operation> operationList = operationRepository.findAllByInterfaceIdAndDeletedDateIsNull(matchedInterfaceId);
        log.info("[СПИСОК_OPERATION] Ветка {}. Найдено операций для interfaceId={}: {}. ID: {}",
                branch.getBranchName(),
                matchedInterfaceId,
                operationList.size(),
                operationList.stream().map(Operation::getId).toList());
        //все ли в discoveredOperationList элементы connectionOperationId ветки == operation
        List<Integer> operationIds = operationList.stream().map(Operation::getId).toList();
        if (discoveredOperationList.stream()
                .allMatch(ds -> operationIds.contains(branch.getConnectionOperationId(ds)))) {
            branch.setConnectionInterfaceId(discoveredInterface, matchedInterfaceId);
            discoveredInterfaceRepository.save(discoveredInterface);
        }
    }

    private Optional<ArchOperationProjection> attempt(DiscoveredConnectionBranch branch, int number, String name,
                                                      String type, List<Integer> interfaceIds) {
        log.info("[ПОИСК_OPERATION] Ветка {}. Попытка {}. Параметры: name='{}', type='{}', interfaceIds={}",
                branch.getBranchName(), number, name, type, interfaceIds);
        Optional<ArchOperationProjection> operation = archOperationMatchingService.findFirstMatch(name, type, interfaceIds);
        log.info("[РЕЗУЛЬТАТ] Ветка {}. Попытка {}. Операция найдена: {}",
                branch.getBranchName(), number, operation.isPresent());
        return operation;
    }
}
