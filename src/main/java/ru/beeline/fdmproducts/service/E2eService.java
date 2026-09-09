/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.dto.SlaV2DTO;
import ru.beeline.fdmproducts.dto.e2e.*;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.exception.EntityNotFoundException;
import ru.beeline.fdmproducts.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class E2eService {

    private final ProductRepository productRepository;
    private final ContainerRepository containerRepository;
    private final InterfaceRepository interfaceRepository;
    private final OperationRepository operationRepository;
    private final SlaRepository slaRepository;
    private final E2eRepository e2eRepository;
    private final OperationRelationRepository operationRelationRepository;
    private final ProductBranchService productBranchService;

    @Transactional
    public E2eUpsertResponseDTO upsert(E2eUpsertRequestDTO request) {
        log.info("E2E upsert: обработка, e2e.uid={}",
                request != null && request.getE2e() != null ? request.getE2e().getUid() : null);
        validateRequest(request);
        List<E2eProductDTO> products = defaultList(request.getProducts());
        List<E2eContainerDTO> containers = defaultList(request.getContainers());
        List<E2eInterfaceDTO> interfaces = defaultList(request.getInterfaces());
        List<E2eOperationDTO> operations = defaultList(request.getOperations());
        List<E2eOperationRelationDTO> relations = defaultList(request.getOperationsRelations());
        log.info("входные данные, products={}, containers={}, interfaces={}, operations={}, relations={}",
                products.size(), containers.size(), interfaces.size(), operations.size(), relations.size());
        ProductIndex productIndex = upsertProducts(products);
        Map<Integer, Integer> branchIdByProductId = new HashMap<>();
        ContainerIndex containerIndex = upsertContainers(containers, productIndex, branchIdByProductId);
        InterfaceIndex interfaceIndex = upsertInterfaces(interfaces, containerIndex, containers, productIndex, branchIdByProductId);
        Map<String, Integer> operationIdByUid = upsertOperations(
                operations, interfaceIndex, interfaces, containerIndex.byKey().values());
        validateRelations(relations, operationIdByUid);
        E2eInfoDTO e2eInfo = request.getE2e();
        E2e e2e = upsertE2e(e2eInfo);
        rebuildOperationRelations(e2e.getId(), relations, operationIdByUid);
        log.info("E2E upsert: завершён, id={}, code={}", e2e.getId(), e2e.getCode());
        return E2eUpsertResponseDTO.builder()
                .id(e2e.getId())
                .code(e2e.getCode())
                .build();
    }

    private record ProductIndex(Map<String, Product> byCmdb, Map<Long, Product> byVersionId) {}

    private record ContainerIndex(Map<String, ContainerProduct> byKey, Map<Long, ContainerProduct> byVersionId) {}

    private record InterfaceIndex(Map<String, Interface> byKey, Map<Long, Interface> byVersionId) {}

    private void validateRequest(E2eUpsertRequestDTO request) {
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
        validateContainers(defaultList(request.getContainers()));
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

    private void validateContainers(List<E2eContainerDTO> containers) {
        for (E2eContainerDTO container : containers) {
            requireNonBlank(container.getCode(), "containers.code");
            requireNonBlank(container.getName(), "containers.name");
            requireNonBlank(container.getParentProductCmdb(), "containers.parentProductCmdb");
        }
    }

    private void validateInterfaces(List<E2eInterfaceDTO> interfaces) {
        for (E2eInterfaceDTO iface : interfaces) {
            requireNonBlank(iface.getCode(), "interfaces.code");
            requireNonBlank(iface.getName(), "interfaces.name");
            requireNonBlank(iface.getParentContainerCode(), "interfaces.parentContainerCode");
            requireNonBlank(iface.getProtocol(), "interfaces.protocol");
        }
    }

    private void validateOperations(List<E2eOperationDTO> operations) {
        Set<String> uids = new HashSet<>();
        for (E2eOperationDTO operation : operations) {
            requireNonBlank(operation.getUid(), "operations.uid");
            if (!uids.add(operation.getUid())) {
                throw new IllegalArgumentException("Дубликат uid в operations: " + operation.getUid());
            }
            requireNonBlank(operation.getName(), "operations.name");
            requireNonBlank(operation.getType(), "operations.type");
            requireNonBlank(operation.getParentInterfaceCode(), "operations.parentInterfaceCode");
        }
    }

    private void validateRelationsStructure(List<E2eOperationRelationDTO> relations) {
        for (E2eOperationRelationDTO relation : relations) {
            requireNonBlank(relation.getRelatedOperationId(), "operationsRelations.relatedOperationId");
            if (relation.getOrder() == null) {
                throw new IllegalArgumentException("Отсутствует обязательное поле operationsRelations.order");
            }
        }
    }

    private void validateRelations(List<E2eOperationRelationDTO> relations, Map<String, Integer> operationIdByUid) {
        for (E2eOperationRelationDTO relation : relations) {
            if (StringUtils.hasText(relation.getOperationId())
                    && !operationIdByUid.containsKey(relation.getOperationId())) {
                throw new IllegalArgumentException("operationsRelations ссылается на неизвестный uid операции");
            }
            if (!operationIdByUid.containsKey(relation.getRelatedOperationId())) {
                throw new IllegalArgumentException("operationsRelations ссылается на неизвестный uid операции");
            }
        }
    }

    private ProductIndex upsertProducts(List<E2eProductDTO> products) {
        Map<String, Product> byCmdb = new HashMap<>();
        Map<Long, Product> byVersionId = new HashMap<>();
        for (E2eProductDTO dto : products) {
            String cmdbKey = normalizeKey(dto.getCmdb());
            Product product = productRepository.findByAliasCaseInsensitive(dto.getCmdb());
            if (product == null) {
                product = Product.builder()
                        .alias(dto.getCmdb())
                        .name(dto.getName())
                        .build();
                product = productRepository.save(product);
                log.info("Создан product, id={}, cmdb={}, productVersionId={}",
                        product.getId(), dto.getCmdb(), dto.getProductVersionId());
            } else {
                log.info("Найден product, id={}, cmdb={}, productVersionId={}",
                        product.getId(), dto.getCmdb(), dto.getProductVersionId());
            }
            byCmdb.put(cmdbKey, product);
            if (dto.getProductVersionId() != null) {
                byVersionId.put(dto.getProductVersionId(), product);
            }
        }
        return new ProductIndex(byCmdb, byVersionId);
    }

    private ContainerIndex upsertContainers(List<E2eContainerDTO> containers, ProductIndex productIndex,
                                            Map<Integer, Integer> branchIdByProductId) {
        Map<String, ContainerProduct> byKey = new HashMap<>();
        Map<Long, ContainerProduct> byVersionId = new HashMap<>();
        for (E2eContainerDTO dto : containers) {
            Product product = resolveProductForContainer(dto, productIndex);
            if (product == null) {
                throw new IllegalArgumentException("Не найден parentProductCmdb для контейнера: " + dto.getCode());
            }
            Integer productBranchId = resolveMainBranchId(product, branchIdByProductId);
            String productKey = normalizeKey(product.getAlias());
            ContainerProduct container = findContainerInDb(dto.getCode(), dto.getName(), productBranchId);
            if (container == null) {
                container = ContainerProduct.builder()
                        .code(dto.getCode())
                        .name(dto.getName())
                        .productBranchId(productBranchId)
                        .createdDate(new Date())
                        .build();
                container = containerRepository.save(container);
                log.info("Создан container, id={}, code={}, productBranchId={}, productVersionId={}, containerVersionId={}",
                        container.getId(), dto.getCode(), productBranchId,
                        dto.getProductVersionId(), dto.getContainerVersionId());
            } else {
                boolean update = false;
                if (!Objects.equals(container.getName(), dto.getName())) {
                    container.setName(dto.getName());
                    update = true;
                }
                if (container.getDeletedDate() != null) {
                    container.setDeletedDate(null);
                    update = true;
                }
                if (update) {
                    container.setUpdatedDate(new Date());
                    container = containerRepository.save(container);
                    log.info("Обновлён container, id={}, code={}, productBranchId={}, containerVersionId={}",
                            container.getId(), dto.getCode(), productBranchId, dto.getContainerVersionId());
                } else {
                    log.info("Найден container, id={}, code={}, productBranchId={}, containerVersionId={}",
                            container.getId(), dto.getCode(), productBranchId, dto.getContainerVersionId());
                }
            }
            byKey.put(containerKey(productKey, dto.getCode()), container);
            if (dto.getContainerVersionId() != null) {
                byVersionId.put(dto.getContainerVersionId(), container);
            }
        }
        return new ContainerIndex(byKey, byVersionId);
    }

    private Integer resolveMainBranchId(Product product, Map<Integer, Integer> branchIdByProductId) {
        return branchIdByProductId.computeIfAbsent(product.getId(),
                id -> productBranchService.getOrCreateId(product.getAlias(), null));
    }

    private Product resolveProductForContainer(E2eContainerDTO dto, ProductIndex productIndex) {
        if (dto.getProductVersionId() != null) {
            Product product = productIndex.byVersionId().get(dto.getProductVersionId());
            if (product != null) {
                return product;
            }
            log.warn("productVersionId={} для контейнера {} не найден среди products[] запроса — fallback на parentProductCmdb",
                    dto.getProductVersionId(), dto.getCode());
        }
        String productKey = normalizeKey(dto.getParentProductCmdb());
        Product product = productIndex.byCmdb().get(productKey);
        if (product == null) {
            product = productRepository.findByAliasCaseInsensitive(dto.getParentProductCmdb());
        }
        return product;
    }

    private InterfaceIndex upsertInterfaces(List<E2eInterfaceDTO> interfaces,
                                             ContainerIndex containerIndex,
                                             List<E2eContainerDTO> containerDtos,
                                             ProductIndex productIndex,
                                             Map<Integer, Integer> branchIdByProductId) {
        Map<String, String> containerToProduct = buildContainerToProductMap(containerDtos);
        Map<String, Interface> byKey = new HashMap<>();
        Map<Long, Interface> byVersionId = new HashMap<>();
        for (E2eInterfaceDTO dto : interfaces) {
            ContainerProduct container = resolveContainerForInterface(dto, containerIndex, containerToProduct, productIndex,
                    branchIdByProductId);
            if (container == null) {
                throw new IllegalArgumentException("Не найден parentContainerCode для интерфейса: " + dto.getCode());
            }
            registerContainer(containerIndex.byKey(), productIndex.byCmdb(), container, branchIdByProductId);
            Interface iface = findInterfaceInDb(dto.getCode(), dto.getName(), container.getId());
            if (iface == null) {
                iface = Interface.builder()
                        .code(dto.getCode())
                        .name(dto.getName())
                        .containerId(container.getId())
                        .protocol(dto.getProtocol())
                        .specLink(dto.getSpecLink())
                        .version(dto.getVersion())
                        .createdDate(LocalDateTime.now())
                        .build();
                iface = interfaceRepository.save(iface);
                log.info("Создан interface, id={}, code={}, containerId={}, containerVersionId={}, interfaceVersionId={}",
                        iface.getId(), dto.getCode(), container.getId(),
                        dto.getContainerVersionId(), dto.getInterfaceVersionId());
            } else {
                updateInterface(iface, dto);
            }
            byKey.put(interfaceKey(container.getId(), dto.getCode()), iface);
            if (dto.getInterfaceVersionId() != null) {
                byVersionId.put(dto.getInterfaceVersionId(), iface);
            }
        }
        return new InterfaceIndex(byKey, byVersionId);
    }

    private Map<String, String> buildContainerToProductMap(List<E2eContainerDTO> containerDtos) {
        Map<String, String> containerToProduct = new HashMap<>();
        for (E2eContainerDTO c : containerDtos) {
            // при одинаковых code оставляем первый; однозначность даёт containerVersionId
            containerToProduct.putIfAbsent(normalizeKey(c.getCode()), normalizeKey(c.getParentProductCmdb()));
        }
        return containerToProduct;
    }

    private ContainerProduct resolveContainerForInterface(E2eInterfaceDTO dto, ContainerIndex containerIndex,
                                                           Map<String, String> containerToProduct, ProductIndex productIndex,
                                                           Map<Integer, Integer> branchIdByProductId) {
        if (dto.getContainerVersionId() != null) {
            ContainerProduct container = containerIndex.byVersionId().get(dto.getContainerVersionId());
            if (container != null) {
                return container;
            }
            log.warn("containerVersionId={} для интерфейса {} не найден среди containers[] запроса — fallback на parentContainerCode",
                    dto.getContainerVersionId(), dto.getCode());
        }
        String containerCodeKey = normalizeKey(dto.getParentContainerCode());
        String productKey = containerToProduct.get(containerCodeKey);

        ContainerProduct container = null;
        if (productKey != null) {
            container = resolveContainer(dto.getParentContainerCode(), productKey, productIndex.byCmdb(), containerIndex.byKey(),
                    branchIdByProductId);
        }
        if (container == null) {
            container = resolveContainerAcrossProducts(dto.getParentContainerCode(), productIndex.byCmdb(), containerIndex.byKey(),
                    branchIdByProductId);
        }
        return container;
    }

    private void updateInterface(Interface iface, E2eInterfaceDTO dto) {
        boolean update = false;
        if (!Objects.equals(iface.getName(), dto.getName())) {
            iface.setName(dto.getName());
            update = true;
        }
        if (!Objects.equals(iface.getProtocol(), dto.getProtocol())) {
            iface.setProtocol(dto.getProtocol());
            update = true;
        }
        if (!Objects.equals(iface.getSpecLink(), dto.getSpecLink())) {
            iface.setSpecLink(dto.getSpecLink());
            update = true;
        }
        if (!Objects.equals(iface.getVersion(), dto.getVersion())) {
            iface.setVersion(dto.getVersion());
            update = true;
        }
        if (iface.getDeletedDate() != null) {
            iface.setDeletedDate(null);
            update = true;
        }
        if (update) {
            iface.setUpdatedDate(LocalDateTime.now());
            log.info("Обновлён interface, id={}", iface.getId());
            interfaceRepository.save(iface);
        }
    }

    private Map<String, Integer> upsertOperations(List<E2eOperationDTO> operations, InterfaceIndex interfaceIndex,
                                                  List<E2eInterfaceDTO> interfaceDtos, Collection<ContainerProduct> containers) {
        Map<String, Integer> interfaceCodeToContainerId = buildInterfaceCodeToContainerIdMap(interfaceDtos, interfaceIndex);
        Map<String, Integer> operationIdByUid = new HashMap<>();
        for (E2eOperationDTO dto : operations) {
            Interface iface = resolveInterfaceForOperation(
                    dto, interfaceIndex, interfaceCodeToContainerId, containers);
            Operation operation = operationRepository
                    .findByNameAndTypeAndInterfaceId(dto.getName(), dto.getType(), iface.getId())
                    .orElse(null);
            if (operation == null) {
                operation = Operation.builder()
                        .name(dto.getName())
                        .type(dto.getType())
                        .description(dto.getDescription())
                        .interfaceId(iface.getId())
                        .createdDate(LocalDateTime.now())
                        .build();
                operation = operationRepository.save(operation);
                log.info("Создана operation, id={}, uid={}, name={}, type={}, interfaceVersionId={}",
                        operation.getId(), dto.getUid(), dto.getName(), dto.getType(), dto.getInterfaceVersionId());
            } else {
                boolean update = false;
                if (dto.getDescription() != null
                        && !Objects.equals(operation.getDescription(), dto.getDescription())) {
                    operation.setDescription(dto.getDescription());
                    update = true;
                }
                if (operation.getDeletedDate() != null) {
                    operation.setDeletedDate(null);
                    update = true;
                }
                if (update) {
                    operation.setUpdatedDate(LocalDateTime.now());
                    operation = operationRepository.save(operation);
                    log.info("Обновлена operation, id={}, uid={}, interfaceVersionId={}",
                            operation.getId(), dto.getUid(), dto.getInterfaceVersionId());
                } else {
                    log.info("Найдена operation, id={}, uid={}, interfaceVersionId={}",
                            operation.getId(), dto.getUid(), dto.getInterfaceVersionId());
                }
            }
            upsertSla(operation.getId(), dto.getSla());
            operationIdByUid.put(dto.getUid(), operation.getId());
        }
        return operationIdByUid;
    }

    private Map<String, Integer> buildInterfaceCodeToContainerIdMap(List<E2eInterfaceDTO> interfaceDtos,
                                                                   InterfaceIndex interfaceIndex) {
        Map<String, Integer> result = new HashMap<>();
        for (E2eInterfaceDTO dto : interfaceDtos) {
            if (dto.getInterfaceVersionId() == null) {
                continue;
            }
            Interface iface = interfaceIndex.byVersionId().get(dto.getInterfaceVersionId());
            if (iface != null) {
                result.putIfAbsent(normalizeKey(dto.getCode()), iface.getContainerId());
            }
        }
        return result;
    }

    private void upsertSla(Integer operationId, E2eOperationSlaDTO slaDto) {
        if (slaDto == null) {
            return;
        }
        Sla sla = slaRepository.findByOperationId(operationId).orElse(null);
        if (sla == null) {
            sla = Sla.builder()
                    .operationId(operationId)
                    .rps(slaDto.getRps())
                    .latency(slaDto.getLatency())
                    .errorRate(slaDto.getErrorRate())
                    .build();
            slaRepository.save(sla);
            log.info("Создан sla, operationId={}, rps={}, latency={}, errorRate={}",
                    operationId, slaDto.getRps(), slaDto.getLatency(), slaDto.getErrorRate());
            return;
        }
        if (!slaValuesEqual(sla, slaDto)) {
            sla.setRps(slaDto.getRps());
            sla.setLatency(slaDto.getLatency());
            sla.setErrorRate(slaDto.getErrorRate());
            slaRepository.save(sla);
            log.info("Обновлён sla, operationId={}, rps={}, latency={}, errorRate={}",
                    operationId, slaDto.getRps(), slaDto.getLatency(), slaDto.getErrorRate());
        }
    }

    private boolean slaValuesEqual(Sla sla, E2eOperationSlaDTO slaDto) {
        return Objects.equals(sla.getRps(), slaDto.getRps())
                && Objects.equals(sla.getLatency(), slaDto.getLatency())
                && Objects.equals(sla.getErrorRate(), slaDto.getErrorRate());
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

    private void rebuildOperationRelations(Integer e2eId, List<E2eOperationRelationDTO> relations,
                                           Map<String, Integer> operationIdByUid) {
        List<OperationRelation> toSave = new ArrayList<>();
        for (E2eOperationRelationDTO relation : relations) {
            Integer operationId = StringUtils.hasText(relation.getOperationId())
                    ? operationIdByUid.get(relation.getOperationId()) : null;
            Integer relatedOperationId = operationIdByUid.get(relation.getRelatedOperationId());
            toSave.add(OperationRelation.builder()
                    .e2eId(e2eId)
                    .operationId(operationId)
                    .relatedOperationId(relatedOperationId)
                    .order(relation.getOrder())
                    .stereoType(relation.getStereoType())
                    .build());
        }
        if (!toSave.isEmpty()) {
            operationRelationRepository.saveAll(toSave);
        }
        log.info("E2E upsert: сохранены operation_relations, e2eId={}, count={}", e2eId, toSave.size());
    }

    private void requireNonBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Отсутствует обязательное поле " + fieldName);
        }
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String containerKey(String productCmdbKey, String containerCode) {
        return productCmdbKey + "::" + normalizeKey(containerCode);
    }

    private String interfaceKey(Integer containerId, String interfaceCode) {
        return containerId + "::" + normalizeKey(interfaceCode);
    }

    private <T> List<T> defaultList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private ContainerProduct resolveContainer(String containerCode, String productKey, Map<String, Product> productByCmdb,
                                              Map<String, ContainerProduct> containerByKey,
                                              Map<Integer, Integer> branchIdByProductId) {
        ContainerProduct container = containerByKey.get(containerKey(productKey, containerCode));
        if (container != null) {
            return container;
        }
        Product product = productByCmdb.get(productKey);
        if (product == null) {
            return null;
        }
        return findContainerInDb(containerCode, resolveMainBranchId(product, branchIdByProductId));
    }

    private ContainerProduct resolveContainerAcrossProducts(String containerCode, Map<String, Product> productByCmdb,
                                                            Map<String, ContainerProduct> containerByKey,
                                                            Map<Integer, Integer> branchIdByProductId) {
        ContainerProduct found = null;
        for (Product product : productByCmdb.values()) {
            ContainerProduct candidate = resolveContainer(
                    containerCode, normalizeKey(product.getAlias()), productByCmdb, containerByKey, branchIdByProductId);
            if (candidate == null) {
                candidate = findContainerInDb(containerCode, resolveMainBranchId(product, branchIdByProductId));
            }
            if (candidate != null) {
                if (found != null && !found.getId().equals(candidate.getId())) {
                    throw new IllegalArgumentException("Неоднозначный parentContainerCode: " + containerCode
                            + ". Укажите containerVersionId");
                }
                found = candidate;
            }
        }
        return found;
    }

    private ContainerProduct findContainerInDb(String containerCode, Integer productBranchId) {
        return containerRepository.findAllByProductBranchIdAndCodeIgnoreCase(productBranchId, containerCode)
                .stream()
                .filter(c -> c.getDeletedDate() == null)
                .findFirst()
                .orElse(null);
    }

    private ContainerProduct findContainerInDb(String containerCode, String containerName, Integer productBranchId) {
        ContainerProduct container = containerRepository
                .findAllByProductBranchIdAndCodeIgnoreCase(productBranchId, containerCode)
                .stream()
                .findFirst()
                .orElse(null);
        if (container != null) {
            return container;
        }
        return containerRepository
                .findAllByProductBranchIdAndCodeIsNullAndNameIgnoreCase(productBranchId, containerName)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Interface findInterfaceInDb(String interfaceCode, String interfaceName, Integer containerId) {
        Interface iface = interfaceRepository
                .findAllByContainerIdAndCodeIgnoreCase(containerId, interfaceCode)
                .stream()
                .findFirst()
                .orElse(null);
        if (iface != null) {
            return iface;
        }
        return interfaceRepository
                .findAllByContainerIdAndCodeIsNullAndNameIgnoreCase(containerId, interfaceName)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void registerContainer(Map<String, ContainerProduct> containerByKey, Map<String, Product> productByCmdb,
                                   ContainerProduct container, Map<Integer, Integer> branchIdByProductId) {
        productByCmdb.values().stream()
                .filter(product -> resolveMainBranchId(product, branchIdByProductId).equals(container.getProductBranchId()))
                .findFirst()
                .ifPresent(product -> containerByKey.putIfAbsent(
                        containerKey(normalizeKey(product.getAlias()), container.getCode()),
                        container));
    }

    private Interface resolveInterfaceForOperation(E2eOperationDTO dto, InterfaceIndex interfaceIndex,
                                                   Map<String, Integer> interfaceCodeToContainerId,
                                                   Collection<ContainerProduct> containers) {
        if (dto.getInterfaceVersionId() != null) {
            Interface iface = interfaceIndex.byVersionId().get(dto.getInterfaceVersionId());
            if (iface != null) {
                if (!normalizeKey(dto.getParentInterfaceCode()).equals(normalizeKey(iface.getCode()))) {
                    throw new IllegalArgumentException(
                            "Не найден parentInterfaceCode для операции: " + dto.getUid());
                }
                return iface;
            }
            log.warn("interfaceVersionId={} для операции {} не найден среди interfaces[] запроса — fallback на parentInterfaceCode",
                    dto.getInterfaceVersionId(), dto.getUid());
        }
        String interfaceCodeKey = normalizeKey(dto.getParentInterfaceCode());
        Integer containerId = interfaceCodeToContainerId.get(interfaceCodeKey);
        if (containerId != null) {
            Interface iface = interfaceIndex.byKey().get(interfaceKey(containerId, dto.getParentInterfaceCode()));
            if (iface != null) {
                return iface;
            }
        }

        List<Interface> matches = new ArrayList<>();
        for (ContainerProduct container : containers) {
            Interface byKey = interfaceIndex.byKey().get(interfaceKey(container.getId(), dto.getParentInterfaceCode()));
            if (byKey != null) {
                matches.add(byKey);
                continue;
            }
            interfaceRepository.findAllByContainerIdAndCodeIgnoreCase(container.getId(), dto.getParentInterfaceCode())
                    .stream()
                    .filter(i -> i.getDeletedDate() == null)
                    .forEach(matches::add);
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Неоднозначный parentInterfaceCode: " + dto.getParentInterfaceCode()
                    + ". Укажите interfaceVersionId");
        }
        throw new IllegalArgumentException("Не найден parentInterfaceCode для операции: " + dto.getUid());
    }

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
                .e2e(mapE2eCard(e2e))
                .operationsRelations(tree)
                .operations(operations)
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
        List<E2eCardResponseDTO> result = e2eList.stream()
                .map(this::mapE2eCard)
                .collect(Collectors.toList());
        log.info("E2E list: завершён, filter={}, count={}", normalizedFilter, result.size());
        return result;
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
