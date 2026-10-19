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
import ru.beeline.fdmproducts.domain.Sla;
import ru.beeline.fdmproducts.dto.SlaV2DTO;
import ru.beeline.fdmproducts.dto.e2e.E2eCardResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eGetResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eOperationCatalogItemDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eRelationTreeNodeDTO;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.E2eSearchRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Чтение e2e API v1. POST /api/v1/e2e удалён (SFDM-4244): последовательность e2e пишется только
 * через POST/PATCH /api/v2/e2e на discovered_operation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class E2eService {

    static final String METHOD_WITH_OPERATION_ID = "Параметры method и operation-id нельзя передавать вместе";
    static final String INVALID_OPERATION_ID = "Параметр operation-id должен быть целым положительным числом";

    private final E2eSearchRepository e2eSearchRepository;
    private final OperationRepository operationRepository;
    private final SlaRepository slaRepository;
    private final E2eRepository e2eRepository;
    private final E2eBranchService e2eBranchService;
    private final E2eOwnerService e2eOwnerService;

    @Transactional
    public E2eGetResponseDTO getByCode(String code, String branch) {
        String branchName = e2eBranchService.resolveBranchName(branch);
        log.info("E2E get: начало, code={}, branch={}", code, branchName);
        E2e e2e = e2eRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new EntityNotFoundException("E2e с указанным кодом не найден"));
        List<OperationRelation> relations = e2eBranchService.relationsOf(e2e, branchName);
        log.info("E2E get: найден e2e id={}, code={}, branch={}, relations={}",
                e2e.getId(), e2e.getCode(), branchName, relations.size());
        List<E2eRelationTreeNodeDTO> tree = relations.isEmpty() ? List.of() : buildBranch(relations, null, new HashSet<>());
        List<E2eOperationCatalogItemDTO> operations = buildOperationsCatalog(tree);
        log.info("E2E get: завершён, code={}, treeNodes={}, operations={}",
                e2e.getCode(), tree.size(), operations.size());
        return E2eGetResponseDTO.builder()
                .e2e(mapE2eCard(e2e, e2eOwnerService.aliasesOf(List.of(e2e))))
                .operationsRelations(tree)
                .operations(operations)
                .build();
    }

    /** Карточка e2e с владельцем: без владельца productId и productAlias равны null. */
    public static E2eCardResponseDTO mapE2eCard(E2e e2e, Map<Integer, String> ownerAliases) {
        return E2eCardResponseDTO.builder()
                .id(e2e.getId())
                .code(e2e.getCode())
                .name(e2e.getName())
                .description(e2e.getDescription())
                .biStepCode(e2e.getBiStepCode())
                .source(e2e.getSource())
                .productId(e2e.getProductId())
                .productAlias(e2e.getProductId() == null ? null : ownerAliases.get(e2e.getProductId()))
                .build();
    }

    private List<E2eRelationTreeNodeDTO> buildBranch(List<OperationRelation> relations, Integer parentOperationId,
                                                      Set<Integer> ancestry) {
        return relations.stream()
                .filter(relation -> Objects.equals(relation.getOperationId(), parentOperationId))
                .sorted(Comparator.comparing(OperationRelation::getOrder))
                .map(relation -> {
                    Integer relatedOperationId = relation.getRelatedOperationId();
                    List<E2eRelationTreeNodeDTO> children;
                    if (relatedOperationId != null && ancestry.contains(relatedOperationId)) {
                        log.warn("E2E get: обнаружена циклическая связь operationId={}, relatedOperationId={} — "
                                + "ветка далее не разворачивается", relation.getOperationId(), relatedOperationId);
                        children = List.of();
                    } else {
                        Set<Integer> nextAncestry = new HashSet<>(ancestry);
                        nextAncestry.add(relatedOperationId);
                        children = buildBranch(relations, relatedOperationId, nextAncestry);
                    }
                    return E2eRelationTreeNodeDTO.builder()
                            .order(relation.getOrder())
                            .relatedOperationId(relatedOperationId)
                            .stereotype(relation.getStereoType())
                            .operationsRelations(children)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<E2eOperationCatalogItemDTO> buildOperationsCatalog(List<E2eRelationTreeNodeDTO> tree) {
        Set<Integer> operationIds = new LinkedHashSet<>();
        collectOperationIds(tree, operationIds);
        if (operationIds.isEmpty()) {
            return List.of();
        }
        List<Integer> operationIdList = new ArrayList<>(operationIds);
        Map<Integer, Sla> slaByOperationId = slaRepository.findAllByOperationIdIn(operationIdList)
                .stream()
                .collect(Collectors.toMap(Sla::getOperationId, sla -> sla, (existing, duplicate) -> existing));
        Map<Integer, E2eOperationCatalogItemDTO> catalogById = operationRepository
                .findOperationsProjection(operationIdList)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ArchOperationProjection::getOpId,
                        projection -> mapOperationCatalogItem(projection, slaByOperationId.get(projection.getOpId())),
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new));
        return operationIds.stream()
                .map(catalogById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private E2eOperationCatalogItemDTO mapOperationCatalogItem(ArchOperationProjection projection, Sla sla) {
        return E2eOperationCatalogItemDTO.builder()
                .id(projection.getOpId())
                .name(projection.getOpName())
                .type(projection.getOpType())
                .interfaceCode(projection.getInterfaceCode())
                .containerCode(projection.getContainerCode())
                .productAlias(projection.getProductAlias())
                .sla(toSlaV2DTO(sla))
                .build();
    }

    private SlaV2DTO toSlaV2DTO(Sla sla) {
        if (sla == null) {
            return null;
        }
        return SlaV2DTO.builder()
                .latency(sla.getLatency())
                .errorRate(sla.getErrorRate())
                .rps(sla.getRps())
                .build();
    }

    private void collectOperationIds(List<E2eRelationTreeNodeDTO> nodes, Set<Integer> operationIds) {
        for (E2eRelationTreeNodeDTO node : nodes) {
            if (node.getRelatedOperationId() != null) {
                operationIds.add(node.getRelatedOperationId());
            }
            if (node.getOperationsRelations() != null && !node.getOperationsRelations().isEmpty()) {
                collectOperationIds(node.getOperationsRelations(), operationIds);
            }
        }
    }

    /**
     * GET /api/v1/e2e (SFDM-4240): список карточек e2e с фильтрами filter, alias, tc-code, method, operation-id.
     * Сначала проверки параметров — до них e2e не читаются; затем один запрос. Совпадений нет — [].
     */
    @Transactional(readOnly = true)
    public List<E2eCardResponseDTO> search(String filter, String alias, String tcCode, String method,
                                           String operationId) {
        E2eSearchRepository.Criteria criteria = criteria(filter, alias, tcCode, method, operationId);
        log.info("E2E list: начало, criteria={}", criteria);
        List<E2eCardResponseDTO> result = e2eSearchRepository.search(criteria);
        log.info("E2E list: завершён, count={}", result.size());
        return result;
    }

    static E2eSearchRepository.Criteria criteria(String filter, String alias, String tcCode, String method,
                                                 String operationId) {
        E2eSearchRepository.BiStepFilter biStepFilter = biStepFilter(filter);
        if (method != null && operationId != null) {
            throw new IllegalArgumentException(METHOD_WITH_OPERATION_ID);
        }
        return new E2eSearchRepository.Criteria(biStepFilter,
                requiredText(alias, "Параметр alias не может быть пустым"),
                requiredText(tcCode, "Параметр tc-code не может быть пустым"),
                requiredText(method, "Параметр method не может быть пустым"),
                positiveId(operationId));
    }

    private static E2eSearchRepository.BiStepFilter biStepFilter(String filter) {
        if (!StringUtils.hasText(filter)) {
            return E2eSearchRepository.BiStepFilter.ALL;
        }
        return switch (filter.trim().toLowerCase(Locale.ROOT)) {
            case "all" -> E2eSearchRepository.BiStepFilter.ALL;
            case "without-bi-step" -> E2eSearchRepository.BiStepFilter.WITHOUT_BI_STEP;
            case "with-bi-step" -> E2eSearchRepository.BiStepFilter.WITH_BI_STEP;
            default -> throw new IllegalArgumentException("Некорректное значение параметра filter");
        };
    }

    private static String requiredText(String value, String message) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    private static Integer positiveId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.matches("\\d{1,10}")) {
            throw new IllegalArgumentException(INVALID_OPERATION_ID);
        }
        long id = Long.parseLong(trimmed);
        if (id <= 0 || id > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(INVALID_OPERATION_ID);
        }
        return (int) id;
    }
}
