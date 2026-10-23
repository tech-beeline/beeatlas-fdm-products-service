/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.E2eBranch;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.repository.E2eBranchRepository;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;

import java.util.List;
import java.util.Optional;

/**
 * Ветка e2e (SFDM-4250) по правилам ветки продукта ({@link ProductBranchService}): не передана — main,
 * пустая — 400, поиск без учёта регистра, имя хранится в нижнем регистре. Карточка e2e одна на code,
 * дерево вызовов висит на ветке.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class E2eBranchService {

    private final E2eBranchRepository e2eBranchRepository;
    private final OperationRelationRepository operationRelationRepository;

    /** Имя ветки из query-параметра branch: null — main, пустое — 400, иначе trim и нижний регистр. */
    public String resolveBranchName(String branch) {
        return ProductBranchService.normalizeBranchName(branch);
    }

    /** Ветка карточки по нормализованному имени; не создаётся. */
    public Optional<E2eBranch> find(String code, String branchName) {
        List<E2eBranch> branches = e2eBranchRepository.findAllByCodeAndBranchNameIgnoreCaseOrderByIdAsc(code, branchName);
        if (branches.size() > 1) {
            // Строки, различающиеся только регистром, могли появиться мимо API. Берём совпадающую с
            // нормализованным именем, иначе самую раннюю.
            log.warn("Ветки e2e различаются только регистром: code={}, branch={}, найдено={}",
                    code, branchName, branches.size());
            return branches.stream()
                    .filter(item -> branchName.equals(item.getBranchName()))
                    .findFirst()
                    .or(() -> Optional.of(branches.get(0)));
        }
        return branches.stream().findFirst();
    }

    /**
     * Ветка карточки, при необходимости созданная, — для POST и PATCH. Сначала регистронезависимый поиск:
     * без него Main завёл бы вторую строку рядом с main, ON CONFLICT опирается на регистрозависимый индекс.
     */
    public E2eBranch getOrCreate(String code, String branchName) {
        return find(code, branchName).orElseGet(() -> {
            Integer id = e2eBranchRepository.upsert(code, branchName);
            log.info("Создана ветка e2e: code={}, branch={}, id={}", code, branchName, id);
            return E2eBranch.builder().id(id).code(code).branchName(branchName).build();
        });
    }

    /** Связи ветки карточки; ветки нет — пустой список, ветка не создаётся. */
    public List<OperationRelation> relationsOf(E2e e2e, String branchName) {
        return find(e2e.getCode(), branchName)
                .map(branch -> operationRelationRepository.findAllByE2eBranchId(branch.getId()))
                .orElse(List.of());
    }
}
