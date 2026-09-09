/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.repository.ContainerRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductBranchRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Общая логика сопоставления метода с архитектурной операцией.
 * <p>
 * Здесь живут обе половины правила: цепочка «продукт → ветка main → контейнеры → интерфейсы»,
 * задающая область поиска, и сам предикат сравнения. Ими пользуются и автоматическое сопоставление
 * discovered-операций ({@link ComparisonOperationsService}), и поиск кандидатов из UI
 * (POST /api/v1/operation/search-matched): у обоих должен получаться один и тот же ответ на вопрос
 * «какая arch-операция соответствует этому методу», иначе UI предложит сопоставление, которого
 * автомат не сделает.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArchOperationMatchingService {

    /** Сопоставление всегда идёт по основной ветке архитектуры продукта. */
    private static final String MAIN_BRANCH = "main";

    private final ProductBranchRepository productBranchRepository;
    private final ContainerRepository containerRepository;
    private final InterfaceRepository interfaceRepository;
    private final OperationRepository operationRepository;

    /**
     * Идентификаторы интерфейсов, среди которых ищется соответствие для продукта. Пустой список
     * означает «искать негде» — у продукта нет ветки main, живых контейнеров или живых интерфейсов.
     *
     * @param productAlias alias продукта; регистр должен быть каноническим (как в product.alias)
     */
    public List<Integer> resolveInterfaceIds(String productAlias) {
        if (productAlias == null) {
            return Collections.emptyList();
        }
        List<Integer> containerIds = productBranchRepository.findByAliasAndBranchName(productAlias, MAIN_BRANCH)
                .map(branch -> containerRepository.findContainerIdsByProductBranchIdAndDeletedDateIsNull(branch.getId()))
                .orElse(Collections.emptyList());
        if (containerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return interfaceRepository.findAllByContainerIdInAndDeletedDateIsNull(containerIds).stream()
                .map(Interface::getId)
                .toList();
    }

    /**
     * Все операции, соответствующие методу. type и protocol необязательны: null снимает фильтр.
     * Пустая область поиска или пустое имя — пустой результат без обращения к БД.
     */
    public List<ArchOperationProjection> findMatches(String name, String type, String protocol,
                                                     List<Integer> interfaceIds) {
        if (name == null || name.isBlank() || interfaceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return operationRepository.findMatchedArchOperations(name, type, protocol, interfaceIds);
    }

    /**
     * Первое совпадение для автоматического сопоставления. Пустой type здесь не означает «любой тип»:
     * так же, как раньше в предикате {@code o.type ILIKE NULL}, операция без известного типа не
     * сопоставляется ни с чем — сопоставить метод по одному имени автомат не вправе.
     */
    public Optional<ArchOperationProjection> findFirstMatch(String name, String type, List<Integer> interfaceIds) {
        if (type == null || type.isBlank()) {
            return Optional.empty();
        }
        return findMatches(name, type, null, interfaceIds).stream().findFirst();
    }
}
