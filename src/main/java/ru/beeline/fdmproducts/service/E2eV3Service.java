/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.dto.e2e.E2eV2DiscoveredOperationCatalogItemDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV3GetResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV3RelationTreeNodeDTO;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.E2eRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GET /api/v3/e2e/{code} (SFDM-4239). Узел дерева — id product.discovered_operation; operation_id корня пуст.
 * Чтение: e2e, владелец, связи одним запросом, карточки discovered-методов одним запросом, product.operation,
 * sla, interface и containers_product — по одному запросу на непустой набор id. Запроса на узел нет.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class E2eV3Service {

    static final String E2E_NOT_FOUND = "E2e с указанным кодом не найден";

    private static final Comparator<OperationRelation> STEP_ORDER = Comparator
            .comparing(OperationRelation::getOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(OperationRelation::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final E2eRepository e2eRepository;
    private final E2eBranchService e2eBranchService;
    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final E2eDiscoveredOperationCatalog discoveredOperationCatalog;
    private final E2eOwnerService e2eOwnerService;

    @Transactional(readOnly = true)
    public E2eV3GetResponseDTO getByCode(String code, String branch) {
        String branchName = e2eBranchService.resolveBranchName(branch);
        log.info("E2E v3 get: начало, code={}, branch={}", code, branchName);
        E2e e2e = e2eRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new EntityNotFoundException(E2E_NOT_FOUND));

        List<OperationRelation> relations = e2eBranchService.relationsOf(e2e, branchName);
        Map<Integer, List<OperationRelation>> childrenByCaller = new HashMap<>();
        List<OperationRelation> roots = new ArrayList<>();
        for (OperationRelation relation : relations) {
            if (relation.getOperationId() == null) {
                roots.add(relation);
            } else {
                childrenByCaller.computeIfAbsent(relation.getOperationId(), key -> new ArrayList<>()).add(relation);
            }
        }
        List<E2eV3RelationTreeNodeDTO> tree = branch(roots, childrenByCaller, new HashSet<>());

        Set<Integer> discoveredIds = new LinkedHashSet<>();
        collectIds(tree, discoveredIds);
        List<E2eV2DiscoveredOperationCatalogItemDTO> discoveredOperations = discoveredIds.isEmpty() ? List.of()
                : discoveredOperationCatalog.build(discoveredIds,
                        discoveredOperationRepository.findDiscoveredOperationCards(discoveredIds));

        log.info("E2E v3 get: завершён, code={}, relations={}, roots={}, discoveredOperations={}",
                e2e.getCode(), relations.size(), tree.size(), discoveredOperations.size());
        return E2eV3GetResponseDTO.builder()
                .e2e(E2eService.mapE2eCard(e2e, e2eOwnerService.aliasesOf(List.of(e2e))))
                .operationsRelations(tree)
                .discoveredOperations(discoveredOperations)
                .build();
    }

    /** ancestry — relatedOperationId узлов на пути от корня: повтор обрывает ветку, operationsRelations = []. */
    private List<E2eV3RelationTreeNodeDTO> branch(List<OperationRelation> steps,
                                                  Map<Integer, List<OperationRelation>> childrenByCaller,
                                                  Set<Integer> ancestry) {
        List<E2eV3RelationTreeNodeDTO> nodes = new ArrayList<>();
        steps.stream().sorted(STEP_ORDER).forEach(relation -> {
            Integer relatedOperationId = relation.getRelatedOperationId();
            List<E2eV3RelationTreeNodeDTO> children;
            if (ancestry.contains(relatedOperationId)) {
                log.warn("E2E v3 get: цикл по discovered_operation id={} — ветка не разворачивается, relationId={}",
                        relatedOperationId, relation.getId());
                children = new ArrayList<>();
            } else {
                Set<Integer> path = new HashSet<>(ancestry);
                path.add(relatedOperationId);
                children = branch(childrenByCaller.getOrDefault(relatedOperationId, List.of()), childrenByCaller, path);
            }
            nodes.add(E2eV3RelationTreeNodeDTO.builder()
                    .order(relation.getOrder())
                    .relatedOperationId(relatedOperationId)
                    .stereotype(StringUtils.hasText(relation.getStereoType()) ? relation.getStereoType() : null)
                    .operationsRelations(children)
                    .build());
        });
        return nodes;
    }

    private static void collectIds(List<E2eV3RelationTreeNodeDTO> nodes, Set<Integer> ids) {
        for (E2eV3RelationTreeNodeDTO node : nodes) {
            if (node.getRelatedOperationId() != null) {
                ids.add(node.getRelatedOperationId());
            }
            collectIds(node.getOperationsRelations(), ids);
        }
    }
}
