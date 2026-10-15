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
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.util.ArrayList;
import java.util.Collection;
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

    private final OperationRepository operationRepository;
    private final SlaRepository slaRepository;
    private final E2eRepository e2eRepository;
    private final OperationRelationRepository operationRelationRepository;
    private final E2eOwnerService e2eOwnerService;

    @Transactional
    public E2eGetResponseDTO getByCode(String code) {
        log.info("E2E get: начало, code={}", code);
        E2e e2e = e2eRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new EntityNotFoundException("E2e с указанным кодом не найден"));
        List<OperationRelation> relations = operationRelationRepository.findAllByE2eId(e2e.getId());
        log.info("E2E get: найден e2e id={}, code={}, relations={}", e2e.getId(), e2e.getCode(), relations.size());
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

    @Transactional
    public List<E2eCardResponseDTO> listE2eByFilter(String filter) {
        String normalizedFilter = normalizeListFilter(filter);
        log.info("E2E list: начало, filter={}", normalizedFilter);
        List<E2e> e2eList = switch (normalizedFilter) {
            case "without-bi-step" -> e2eRepository.findAllWithoutBiStepCode();
            case "with-bi-step" -> e2eRepository.findAllWithBiStepCode();
            default -> e2eRepository.findAll();
        };
        List<E2eCardResponseDTO> result = cards(e2eList);
        log.info("E2E list: завершён, filter={}, count={}", normalizedFilter, result.size());
        return result;
    }

    private List<E2eCardResponseDTO> cards(Collection<E2e> e2eList) {
        Map<Integer, String> ownerAliases = e2eOwnerService.aliasesOf(e2eList);
        return e2eList.stream()
                .map(e2e -> mapE2eCard(e2e, ownerAliases))
                .collect(Collectors.toList());
    }

    private String normalizeListFilter(String filter) {
        if (!StringUtils.hasText(filter) || "all".equalsIgnoreCase(filter.trim())) {
            return "all";
        }
        String value = filter.trim().toLowerCase(Locale.ROOT);
        if ("without-bi-step".equals(value) || "with-bi-step".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("Некорректное значение параметра filter");
    }
}
