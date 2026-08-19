/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.OperationRelation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.Sla;
import ru.beeline.fdmproducts.dto.SlaV2DTO;
import ru.beeline.fdmproducts.dto.e2e.E2eCardResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eInfoDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eOperationCatalogItemDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eOperationSlaDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eProductDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eUpsertResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2DiscoveredOperationCatalogItemDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2GetResponseDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2InterfaceDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2OperationDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2OperationRelationDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2RelationTreeNodeDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2UpsertRequestDTO;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.dto.search.projection.DiscoveredOperationProjection;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.E2eRepository;
import ru.beeline.fdmproducts.repository.OperationRelationRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;
import ru.beeline.fdmproducts.repository.SlaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class E2eV2Service {

    private static final String SOURCE_SPARX = "SPARX";
    private static final String ENTITY_TYPE_DISCOVERED_OPERATION = "discovered_operation";

    private final ProductRepository productRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;
    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final E2eRepository e2eRepository;
    private final OperationRelationRepository operationRelationRepository;
    private final OperationRepository operationRepository;
    private final SlaRepository slaRepository;

    @Transactional
    public E2eUpsertResponseDTO upsert(E2eV2UpsertRequestDTO request) {
        log.info("E2E v2 upsert: обработка, e2e.uid={}",
                request != null && request.getE2e() != null ? request.getE2e().getUid() : null);
        validateRequest(request);
        List<E2eProductDTO> products = defaultList(request.getProducts());
        List<E2eV2InterfaceDTO> interfaces = defaultList(request.getInterfaces());
        List<E2eV2OperationDTO> operations = defaultList(request.getOperations());
        List<E2eV2OperationRelationDTO> relations = defaultList(request.getOperationsRelations());
        log.info("входные данные, products={}, interfaces={}, operations={}, relations={}",
                products.size(), interfaces.size(), operations.size(), relations.size());
        Map<String, Product> productByCmdb = upsertProducts(products);
        Map<String, List<DiscoveredInterface>> interfacesByCode = upsertInterfaces(interfaces, productByCmdb);
        Map<String, Integer> operationIdByUid = upsertOperations(operations, interfacesByCode);
        validateRelations(relations, operationIdByUid);
        E2eInfoDTO e2eInfo = request.getE2e();
        E2e e2e = upsertE2e(e2eInfo);
        rebuildOperationRelations(e2e.getId(), relations, operationIdByUid);
        log.info("E2E v2 upsert: завершён, id={}, code={}", e2e.getId(), e2e.getCode());
        return E2eUpsertResponseDTO.builder()
                .id(e2e.getId())
                .code(e2e.getCode())
                .build();
    }

    private void validateRequest(E2eV2UpsertRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Отсутствует тело запроса");
        }
        if (request.getE2e() == null) {
            throw new IllegalArgumentException("Отсутствует обязательное поле e2e");
        }
        requireNonBlank(request.getE2e().getUid(), "e2e.uid");
        if (request.getOperationsRelations() == null) {
            throw new IllegalArgumentException("Отсутствует обязательное поле operationsRelations");
        }
        validateProducts(defaultList(request.getProducts()));
        validateInterfaces(defaultList(request.getInterfaces()));
        validateOperations(defaultList(request.getOperations()));
        validateRelationsStructure(defaultList(request.getOperationsRelations()));
    }

    private void validateProducts(List<E2eProductDTO> products) {
        for (E2eProductDTO product : products) {
            requireNonBlank(product.getCmdb(), "products.cmdb");
            requireNonBlank(product.getName(), "products.name");
        }
    }

    private void validateInterfaces(List<E2eV2InterfaceDTO> interfaces) {
        for (E2eV2InterfaceDTO iface : interfaces) {
            requireNonBlank(iface.getCode(), "interfaces.code");
            requireNonBlank(iface.getName(), "interfaces.name");
            requireNonBlank(iface.getParentProductCmdb(), "interfaces.parentProductCmdb");
        }
    }

    private void validateOperations(List<E2eV2OperationDTO> operations) {
        Set<String> uids = new HashSet<>();
        for (E2eV2OperationDTO operation : operations) {
            requireNonBlank(operation.getUid(), "operations.uid");
            if (!uids.add(operation.getUid())) {
                throw new IllegalArgumentException("Дубликат uid в operations: " + operation.getUid());
            }
            requireNonBlank(operation.getName(), "operations.name");
            requireNonBlank(operation.getType(), "operations.type");
            requireNonBlank(operation.getParentInterfaceCode(), "operations.parentInterfaceCode");
        }
    }

    private void validateRelationsStructure(List<E2eV2OperationRelationDTO> relations) {
        for (E2eV2OperationRelationDTO relation : relations) {
            requireNonBlank(relation.getRelatedOperationId(), "operationsRelations.relatedOperationId");
            if (relation.getOrder() == null) {
                throw new IllegalArgumentException("Отсутствует обязательное поле operationsRelations.order");
            }
        }
    }

    private void validateRelations(List<E2eV2OperationRelationDTO> relations, Map<String, Integer> operationIdByUid) {
        for (E2eV2OperationRelationDTO relation : relations) {
            if (StringUtils.hasText(relation.getOperationId())
                    && !operationIdByUid.containsKey(relation.getOperationId())) {
                throw new IllegalArgumentException("operationsRelations ссылается на неизвестный uid операции");
            }
            if (!operationIdByUid.containsKey(relation.getRelatedOperationId())) {
                throw new IllegalArgumentException("operationsRelations ссылается на неизвестный uid операции");
            }
        }
    }

    private Map<String, Product> upsertProducts(List<E2eProductDTO> products) {
        Map<String, Product> byCmdb = new HashMap<>();
        for (E2eProductDTO dto : products) {
            String cmdbKey = normalizeKey(dto.getCmdb());
            Product product = productRepository.findByAliasCaseInsensitive(dto.getCmdb());
            if (product == null) {
                product = Product.builder()
                        .alias(dto.getCmdb())
                        .name(dto.getName())
                        .build();
                product = productRepository.save(product);
                log.info("Создан product, id={}, cmdb={}", product.getId(), dto.getCmdb());
            } else {
                log.info("Найден product, id={}, cmdb={}", product.getId(), dto.getCmdb());
            }
            byCmdb.put(cmdbKey, product);
        }
        return byCmdb;
    }

    private Map<String, List<DiscoveredInterface>> upsertInterfaces(List<E2eV2InterfaceDTO> interfaces,
                                                                      Map<String, Product> productByCmdb) {
        Map<String, List<DiscoveredInterface>> byCode = new HashMap<>();
        for (E2eV2InterfaceDTO dto : interfaces) {
            Product product = resolveProduct(dto.getParentProductCmdb(), productByCmdb);
            if (product == null) {
                throw new IllegalArgumentException("Не найден parentProductCmdb для интерфейса: " + dto.getCode());
            }
            DiscoveredInterface iface = discoveredInterfaceRepository
                    .findBySourceAndProductIdAndExternalId(SOURCE_SPARX, product.getId(), dto.getCode())
                    .orElse(null);
            if (iface == null) {
                iface = DiscoveredInterface.builder()
                        .externalId(dto.getCode())
                        .name(dto.getName())
                        .product(product)
                        .apiLink(dto.getSpecLink())
                        .version(dto.getVersion())
                        .source(SOURCE_SPARX)
                        .context(null)
                        .createdDate(LocalDateTime.now())
                        .build();
                iface = discoveredInterfaceRepository.save(iface);
                log.info("Создан discovered_interface, id={}, code={}, productId={}",
                        iface.getId(), dto.getCode(), product.getId());
            } else {
                updateInterface(iface, dto);
            }
            byCode.computeIfAbsent(normalizeKey(dto.getCode()), key -> new ArrayList<>()).add(iface);
        }
        return byCode;
    }

    private void updateInterface(DiscoveredInterface iface, E2eV2InterfaceDTO dto) {
        boolean update = false;
        if (dto.getName() != null && !Objects.equals(iface.getName(), dto.getName())) {
            iface.setName(dto.getName());
            update = true;
        }
        if (dto.getSpecLink() != null && !Objects.equals(iface.getApiLink(), dto.getSpecLink())) {
            iface.setApiLink(dto.getSpecLink());
            update = true;
        }
        if (dto.getVersion() != null && !Objects.equals(iface.getVersion(), dto.getVersion())) {
            iface.setVersion(dto.getVersion());
            update = true;
        }
        if (iface.getDeletedDate() != null) {
            iface.setDeletedDate(null);
            update = true;
        }
        if (update) {
            iface.setUpdatedDate(LocalDateTime.now());
            log.info("Обновлён discovered_interface, id={}", iface.getId());
            discoveredInterfaceRepository.save(iface);
        }
    }

    private Product resolveProduct(String cmdb, Map<String, Product> productByCmdb) {
        Product product = productByCmdb.get(normalizeKey(cmdb));
        if (product != null) {
            return product;
        }
        return productRepository.findByAliasCaseInsensitive(cmdb);
    }

    private Map<String, Integer> upsertOperations(List<E2eV2OperationDTO> operations,
                                                    Map<String, List<DiscoveredInterface>> interfacesByCode) {
        Map<String, Integer> operationIdByUid = new HashMap<>();
        for (E2eV2OperationDTO dto : operations) {
            DiscoveredInterface iface = resolveInterface(dto.getParentInterfaceCode(), interfacesByCode);
            DiscoveredOperation operation = discoveredOperationRepository
                    .findByInterfaceIdAndNameAndType(iface.getId(), dto.getName(), dto.getType())
                    .orElse(null);
            if (operation == null) {
                DiscoveredOperation.DiscoveredOperationBuilder builder = DiscoveredOperation.builder()
                        .name(dto.getName())
                        .type(dto.getType())
                        .description(dto.getDescription())
                        .discoveredInterface(iface)
                        .context(null)
                        .createdDate(LocalDateTime.now());
                applySla(builder, dto.getSla());
                operation = discoveredOperationRepository.save(builder.build());
                log.info("Создана discovered_operation, id={}, uid={}, name={}, type={}",
                        operation.getId(), dto.getUid(), dto.getName(), dto.getType());
            } else {
                boolean update = false;
                if (dto.getDescription() != null && !Objects.equals(operation.getDescription(), dto.getDescription())) {
                    operation.setDescription(dto.getDescription());
                    update = true;
                }
                if (dto.getSla() != null && !slaValuesEqual(operation, dto.getSla())) {
                    operation.setRps(dto.getSla().getRps());
                    operation.setLatency(dto.getSla().getLatency());
                    operation.setErrorRate(dto.getSla().getErrorRate());
                    update = true;
                }
                if (operation.getDeletedDate() != null) {
                    operation.setDeletedDate(null);
                    update = true;
                }
                if (update) {
                    operation.setUpdatedDate(LocalDateTime.now());
                    operation = discoveredOperationRepository.save(operation);
                    log.info("Обновлена discovered_operation, id={}, uid={}", operation.getId(), dto.getUid());
                } else {
                    log.info("Найдена discovered_operation, id={}, uid={}", operation.getId(), dto.getUid());
                }
            }
            operationIdByUid.put(dto.getUid(), operation.getId());
        }
        return operationIdByUid;
    }

    private void applySla(DiscoveredOperation.DiscoveredOperationBuilder builder, E2eOperationSlaDTO sla) {
        if (sla == null) {
            return;
        }
        builder.rps(sla.getRps()).latency(sla.getLatency()).errorRate(sla.getErrorRate());
    }

    private boolean slaValuesEqual(DiscoveredOperation operation, E2eOperationSlaDTO sla) {
        return Objects.equals(operation.getRps(), sla.getRps())
                && Objects.equals(operation.getLatency(), sla.getLatency())
                && Objects.equals(operation.getErrorRate(), sla.getErrorRate());
    }

    private DiscoveredInterface resolveInterface(String code, Map<String, List<DiscoveredInterface>> interfacesByCode) {
        List<DiscoveredInterface> inRequest = interfacesByCode.get(normalizeKey(code));
        if (inRequest != null) {
            if (inRequest.size() == 1) {
                return inRequest.get(0);
            }
            throw new IllegalArgumentException("Неоднозначный parentInterfaceCode: " + code
                    + ". Интерфейс с таким code передан для нескольких продуктов");
        }
        List<DiscoveredInterface> dbMatches = discoveredInterfaceRepository
                .findAllBySourceAndExternalId(SOURCE_SPARX, code)
                .stream()
                .filter(candidate -> candidate.getDeletedDate() == null)
                .toList();
        if (dbMatches.size() == 1) {
            return dbMatches.get(0);
        }
        if (dbMatches.size() > 1) {
            throw new IllegalArgumentException("Неоднозначный parentInterfaceCode: " + code);
        }
        throw new IllegalArgumentException("Не найден parentInterfaceCode для операции: " + code);
    }

    private E2e upsertE2e(E2eInfoDTO e2eInfo) {
        E2e e2e = e2eRepository.findByCode(e2eInfo.getUid()).orElse(null);
        if (e2e == null) {
            e2e = E2e.builder()
                    .code(e2eInfo.getUid())
                    .name(e2eInfo.getName())
                    .description(e2eInfo.getDescription())
                    .biStepCode(e2eInfo.getBiStepCode())
                    .build();
            e2e = e2eRepository.save(e2e);
            log.info("Создан e2e, id={}, code={}", e2e.getId(), e2e.getCode());
        } else {
            if (e2eInfo.getName() != null) {
                e2e.setName(e2eInfo.getName());
            }
            if (e2eInfo.getDescription() != null) {
                e2e.setDescription(e2eInfo.getDescription());
            }
            if (e2eInfo.getBiStepCode() != null) {
                e2e.setBiStepCode(e2eInfo.getBiStepCode());
            }
            e2e = e2eRepository.save(e2e);
            operationRelationRepository.deleteAllByE2eId(e2e.getId());
            log.info("Обновлён e2e, id={}, code={}, старые operation_relations удалены",
                    e2e.getId(), e2e.getCode());
        }
        return e2e;
    }

    private void rebuildOperationRelations(Integer e2eId, List<E2eV2OperationRelationDTO> relations,
                                            Map<String, Integer> operationIdByUid) {
        List<OperationRelation> toSave = new ArrayList<>();
        for (E2eV2OperationRelationDTO relation : relations) {
            Integer operationId = StringUtils.hasText(relation.getOperationId())
                    ? operationIdByUid.get(relation.getOperationId()) : null;
            Integer relatedOperationId = operationIdByUid.get(relation.getRelatedOperationId());
            toSave.add(OperationRelation.builder()
                    .e2eId(e2eId)
                    .operationId(operationId)
                    .relatedOperationId(relatedOperationId)
                    .order(relation.getOrder())
                    .stereoType(relation.getStereoType())
                    .entityTypeOperation(ENTITY_TYPE_DISCOVERED_OPERATION)
                    .entityTypeOperationRelation(ENTITY_TYPE_DISCOVERED_OPERATION)
                    .build());
        }
        if (!toSave.isEmpty()) {
            operationRelationRepository.saveAll(toSave);
        }
        log.info("E2E v2 upsert: сохранены operation_relations, e2eId={}, count={}", e2eId, toSave.size());
    }

    @Transactional
    public E2eV2GetResponseDTO getByCode(String code) {
        log.info("E2E v2 get: начало, code={}", code);
        E2e e2e = e2eRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new EntityNotFoundException("E2e с указанным кодом не найден"));
        List<OperationRelation> relations = operationRelationRepository.findAllByE2eId(e2e.getId());
        log.info("E2E v2 get: найден e2e id={}, code={}, relations={}", e2e.getId(), e2e.getCode(), relations.size());
        List<E2eV2RelationTreeNodeDTO> tree = relations.isEmpty()
                ? List.of() : buildBranch(relations, null, null, new HashSet<>());
        Set<Integer> operationIds = new LinkedHashSet<>();
        Set<Integer> discoveredOperationIds = new LinkedHashSet<>();
        collectOperationIds(tree, operationIds, discoveredOperationIds);
        List<E2eOperationCatalogItemDTO> operations = buildOperationsCatalog(operationIds);
        List<E2eV2DiscoveredOperationCatalogItemDTO> discoveredOperations = buildDiscoveredOperationsCatalog(discoveredOperationIds);
        log.info("E2E v2 get: завершён, code={}, treeNodes={}, operations={}, discoveredOperations={}",
                e2e.getCode(), tree.size(), operations.size(), discoveredOperations.size());
        return E2eV2GetResponseDTO.builder()
                .e2e(mapE2eCard(e2e))
                .operationsRelations(tree)
                .operations(operations)
                .discoveredOperations(discoveredOperations)
                .build();
    }

    private E2eCardResponseDTO mapE2eCard(E2e e2e) {
        return E2eCardResponseDTO.builder()
                .id(e2e.getId())
                .code(e2e.getCode())
                .name(e2e.getName())
                .description(e2e.getDescription())
                .biStepCode(e2e.getBiStepCode())
                .build();
    }

    private List<E2eV2RelationTreeNodeDTO> buildBranch(List<OperationRelation> relations, Integer parentOperationId,
                                                         String parentEntityType, Set<String> ancestry) {
        return relations.stream()
                .filter(relation -> parentOperationId == null
                        ? relation.getOperationId() == null
                        : Objects.equals(relation.getOperationId(), parentOperationId)
                            && Objects.equals(relation.getEntityTypeOperation(), parentEntityType))
                .sorted(Comparator.comparing(OperationRelation::getOrder))
                .map(relation -> {
                    Integer relatedOperationId = relation.getRelatedOperationId();
                    String relatedEntityType = relation.getEntityTypeOperationRelation();
                    String ancestryKey = relatedEntityType + "::" + relatedOperationId;
                    List<E2eV2RelationTreeNodeDTO> children;
                    if (relatedOperationId != null && ancestry.contains(ancestryKey)) {
                        log.warn("E2E v2 get: обнаружена циклическая связь operationId={}, entityType={}, "
                                        + "relatedOperationId={}, relatedEntityType={} — ветка далее не разворачивается",
                                relation.getOperationId(), relation.getEntityTypeOperation(),
                                relatedOperationId, relatedEntityType);
                        children = List.of();
                    } else {
                        Set<String> nextAncestry = new HashSet<>(ancestry);
                        nextAncestry.add(ancestryKey);
                        children = buildBranch(relations, relatedOperationId, relatedEntityType, nextAncestry);
                    }
                    return E2eV2RelationTreeNodeDTO.builder()
                            .order(relation.getOrder())
                            .relatedOperationId(relatedOperationId)
                            .stereotype(relation.getStereoType())
                            .entityTypeRelatedOperation(relatedEntityType)
                            .operationsRelations(children)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void collectOperationIds(List<E2eV2RelationTreeNodeDTO> nodes, Set<Integer> operationIds,
                                      Set<Integer> discoveredOperationIds) {
        for (E2eV2RelationTreeNodeDTO node : nodes) {
            if (node.getRelatedOperationId() != null) {
                if (ENTITY_TYPE_DISCOVERED_OPERATION.equals(node.getEntityTypeRelatedOperation())) {
                    discoveredOperationIds.add(node.getRelatedOperationId());
                } else {
                    operationIds.add(node.getRelatedOperationId());
                }
            }
            if (node.getOperationsRelations() != null && !node.getOperationsRelations().isEmpty()) {
                collectOperationIds(node.getOperationsRelations(), operationIds, discoveredOperationIds);
            }
        }
    }

    private List<E2eOperationCatalogItemDTO> buildOperationsCatalog(Set<Integer> operationIds) {
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

    private List<E2eV2DiscoveredOperationCatalogItemDTO> buildDiscoveredOperationsCatalog(Set<Integer> discoveredOperationIds) {
        if (discoveredOperationIds.isEmpty()) {
            return List.of();
        }
        List<Integer> idList = new ArrayList<>(discoveredOperationIds);
        Map<Integer, E2eV2DiscoveredOperationCatalogItemDTO> catalogById = discoveredOperationRepository
                .findDiscoveredOperationsProjection(idList)
                .stream()
                .collect(Collectors.toMap(
                        DiscoveredOperationProjection::getOpId,
                        this::mapDiscoveredOperationCatalogItem,
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new));
        return discoveredOperationIds.stream()
                .map(catalogById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private E2eV2DiscoveredOperationCatalogItemDTO mapDiscoveredOperationCatalogItem(DiscoveredOperationProjection projection) {
        boolean hasSla = projection.getRps() != null || projection.getLatency() != null || projection.getErrorRate() != null;
        return E2eV2DiscoveredOperationCatalogItemDTO.builder()
                .id(projection.getOpId())
                .name(projection.getOpName())
                .type(projection.getOpType())
                .interfaceCode(projection.getInterfaceCode())
                .productAlias(projection.getProductAlias())
                .source(projection.getSource())
                .sla(hasSla ? SlaV2DTO.builder()
                        .rps(projection.getRps())
                        .latency(projection.getLatency())
                        .errorRate(projection.getErrorRate())
                        .build() : null)
                .build();
    }

    private void requireNonBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Отсутствует обязательное поле " + fieldName);
        }
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private <T> List<T> defaultList(List<T> list) {
        return list == null ? List.of() : list;
    }
}
