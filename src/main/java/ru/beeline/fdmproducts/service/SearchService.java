package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmproducts.dto.search.MatchedArchOperationDTO;
import ru.beeline.fdmproducts.dto.search.OperationMatchCandidateDTO;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.mapper.ArchOperationMapper;
import ru.beeline.fdmproducts.mapper.DiscoveredOperationMapper;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;
import ru.beeline.fdmproducts.repository.ProductRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class SearchService {

    @Autowired
    private OperationRepository operationRepository;

    @Autowired
    private ArchOperationMapper archOperationMapper;

    @Autowired
    private InterfaceRepository interfaceRepository;

    @Autowired
    private DiscoveredOperationMapper discoveredOperationMapper;

    @Autowired
    private DiscoveredOperationRepository discoveredOperationRepository;

    @Autowired
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ArchOperationMatchingService archOperationMatchingService;

    public OperationSearchDTO searchOperations(String path, String type) {
        OperationSearchDTO result = new OperationSearchDTO();
        result.setArchOperations(new ArrayList<>());
        result.setDiscoveredOperations(new ArrayList<>());
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Параметр path не должен быть пустым.");
        }
        path = path.replace("%7B", "{").replace("%7D", "}");
        List<ArchOperationProjection> archOperationProjections = type != null
                ? operationRepository.findArchOperationsProjectionByType(path, type)
                : operationRepository.findArchOperationsProjection(path);

        List<DiscoveredOperation> discoveredOperationList = new ArrayList<>();
        if (archOperationProjections.size() < 50) {
            int limitSearch = 50 - archOperationProjections.size();
            discoveredOperationList = type != null ?
                    discoveredOperationRepository.findAllByNameAndTypeIgnoreCaseAndDeletedDateIsNull(path, type,
                                                                                                     limitSearch)
                    : discoveredOperationRepository.findAllByNameAndDeletedDateIsNull(path, limitSearch);
        }
        if (archOperationProjections.isEmpty() && discoveredOperationList.isEmpty()) {
            return result;
        }
        if (!archOperationProjections.isEmpty()) {
            List<ArchOperationDTO> archOperationDTOList = archOperationProjections.stream()
                    .filter(Objects::nonNull)
                    .map(proj -> archOperationMapper.mapToArchOperationDTO(proj))
                    .toList();
            result.setArchOperations(archOperationDTOList);
        }
        List<DiscoveredOperation> withConOperation = new ArrayList<>();
        List<DiscoveredOperation> withoutConOperation = new ArrayList<>();
        for (DiscoveredOperation obj : discoveredOperationList) {
            if (obj.getConnectionOperationId() == null) {
                withoutConOperation.add(obj);
            } else {
                withConOperation.add(obj);
            }
        }
        Map<Integer, DiscoveredInterface> interfaceMap = getInterfaceMap(withoutConOperation);
        List<DiscoveredOperationDTO> disconnectedDTOs = createDisconnectedDTOs(withoutConOperation, interfaceMap);
        result.getDiscoveredOperations().addAll(disconnectedDTOs);
        List<ArchOperationProjection> disOperationWithConOperation = operationRepository
                .findOperationsProjection(withConOperation.stream()
                        .map(DiscoveredOperation::getConnectionOperationId).toList());
        Map<Integer, ArchOperationProjection> projectionMap = disOperationWithConOperation.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ArchOperationProjection::getOpId,
                        Function.identity(),
                        (first, second) -> first
                ));
        List<DiscoveredOperationDTO> doResult = withConOperation.stream()
                .map(op -> discoveredOperationMapper.mapToOperationDTO(op, projectionMap.get(op.getConnectionOperationId())))
                .toList();
        result.getDiscoveredOperations().addAll(doResult);
        return result;
    }

    private Map<Integer, DiscoveredInterface> getInterfaceMap(List<DiscoveredOperation> discoveredOperations) {
        List<Integer> interfaceIds = discoveredOperations.stream()
                .map(DiscoveredOperation::getInterfaceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<DiscoveredInterface> interfaces = discoveredInterfaceRepository
                .findAllByIdInAndDeletedDateIsNull(interfaceIds);
        return interfaces.stream()
                .collect(Collectors.toMap(DiscoveredInterface::getId, Function.identity()));
    }

    private List<DiscoveredOperationDTO> createDisconnectedDTOs(List<DiscoveredOperation> discoveredOperations,
                                                                Map<Integer, DiscoveredInterface> interfaceMap) {
        return discoveredOperations.stream()
                .map(op -> {
                    DiscoveredInterface discoveredInterface = interfaceMap.get(op.getInterfaceId());
                    if (discoveredInterface == null) {
                        log.warn("Interface not found for operation {} with interfaceId {}",
                                op.getId(), op.getInterfaceId());
                        return null;
                    }
                    Product product = discoveredInterface.getProduct();
                    return DiscoveredOperationDTO.builder()
                            .id(op.getId())
                            .name(op.getName())
                            .type(op.getType())
                            .connectionOperation(null)
                            .interfaceObj(InterfaceSearchDTO.builder()
                                    .id(discoveredInterface.getId())
                                    .name(discoveredInterface.getName())
                                    .build())
                            .container(null)
                            .product(product != null ?
                                    ProductSearchDTO.builder()
                                            .id(product.getId())
                                            .name(product.getName())
                                            .alias(product.getAlias())
                                            .build() :
                                    null
                            )
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Архитектурные операции, сопоставимые кандидатам. Ответ — плоский массив: результаты
     * кандидатов идут подряд в порядке запроса, кандидат без совпадений просто ничего не добавляет.
     * <p>
     * Неизвестный productCode — не ошибка запроса, а состояние каталога: UI показывает его рядом с
     * остальными строками, поэтому статус остаётся 200, а кандидат отдаётся элементом-признаком.
     */
    @Transactional(readOnly = true)
    public List<MatchedArchOperationDTO> searchMatchedOperations(List<OperationMatchCandidateDTO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("В теле запроса не передан обязательный атрибут");
        }
        // Тело проверяется целиком до первого запроса в БД: невалидный кандидат в середине списка
        // не должен отдавать 400 после того, как часть работы уже сделана.
        candidates.forEach(this::validateCandidate);

        Map<String, Optional<Product>> productByCode = new HashMap<>();
        List<MatchedArchOperationDTO> result = new ArrayList<>();
        for (OperationMatchCandidateDTO candidate : candidates) {
            String productCode = candidate.getProductCode().trim();
            Optional<Product> product = productByCode.computeIfAbsent(productCode.toLowerCase(Locale.ROOT),
                    code -> Optional.ofNullable(productRepository.findByAliasCaseInsensitive(productCode)));
            if (product.isEmpty()) {
                log.info("Продукт кандидата не найден: productCode={}", productCode);
                result.add(MatchedArchOperationDTO.builder()
                        .productCode(productCode)
                        .error("Продукт с кодом " + productCode + " не найден")
                        .notFound(true)
                        .build());
                continue;
            }
            List<Integer> interfaceIds = archOperationMatchingService.resolveInterfaceIds(product.get().getAlias());
            archOperationMatchingService.findMatches(candidate.getMethodName().trim(),
                            emptyToNull(candidate.getMethodType()), emptyToNull(candidate.getProtocol()), interfaceIds)
                    .forEach(proj -> result.add(archOperationMapper.mapToMatchedArchOperationDTO(proj, productCode)));
        }
        log.info("Поиск сопоставимых операций: кандидатов={}, элементов в ответе={}", candidates.size(), result.size());
        return result;
    }

    private void validateCandidate(OperationMatchCandidateDTO candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Пустой элемент в теле запроса");
        }
        if (candidate.getMethodName() == null || candidate.getMethodName().isBlank()) {
            throw new IllegalArgumentException("Отсутствует обязательное поле methodName");
        }
        if (candidate.getProductCode() == null || candidate.getProductCode().isBlank()) {
            throw new IllegalArgumentException("Отсутствует обязательное поле productCode");
        }
    }

    /** Незаполненный необязательный фильтр — это его отсутствие, а не поиск по пустой строке. */
    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public List<ArchOperationDTO> getOperationByTc(Integer tcId) {
        List<ArchOperationDTO> result = new ArrayList<>();
        List<Operation> operations = operationRepository.findOperationsWithFullChainGraph(tcId);
        if (!operations.isEmpty()) {
            result = operations.stream()
                    .filter(operation -> {
                        var interf = operation.getInterfaceObj();
                        return interf != null && interf.getContainerProduct() != null;
                    })
                    .map(operation -> archOperationMapper.mapToArchOperationDTO(operation))
                    .toList();
        }
        return result;
    }

    public List<ProductInfoDTOTree> getOperationByTcTree(Integer id) {
        List<Operation> operations = operationRepository.findOperationsWithFullChainGraph(id);
        if (operations.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, ProductInfoDTOTree> productMap = new HashMap<>();
        for (Operation op : operations) {
            var interf = op.getInterfaceObj();
            if (interf == null || interf.getDeletedDate() != null) {
                continue;
            }
            var container = interf.getContainerProduct();
            if (container == null || container.getDeletedDate() != null) {
                continue;
            }
            var productBranch = container.getProductBranch();
            var product = productBranch == null ? null : productBranch.getProduct();
            if (product == null) {
                continue;
            }

            OperationDTOTree opDto = OperationDTOTree.builder()
                    .operationId(op.getId())
                    .operationName(op.getName())
                    .operationType(op.getType())
                    .build();

            InterfaceDTOTree ifaceDto = InterfaceDTOTree.builder()
                    .interfaceId(interf.getId())
                    .interfaceName(interf.getName())
                    .interfaceCode(interf.getCode())
                    .operations(new ArrayList<>(List.of(opDto)))
                    .build();

            ContainerDTOTree contDto = ContainerDTOTree.builder()
                    .containerId(container.getId())
                    .containerName(container.getName())
                    .containerCode(container.getCode())
                    .interfaces(new ArrayList<>(List.of(ifaceDto)))
                    .build();

            ProductInfoDTOTree productDto = productMap.computeIfAbsent(
                    product.getId(),
                    pid -> ProductInfoDTOTree.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .productCode(product.getAlias())
                            .containers(new ArrayList<>())
                            .build()
            );

            ContainerDTOTree existingContainer = productDto.getContainers().stream()
                    .filter(c -> c.getContainerId().equals(container.getId()))
                    .findFirst()
                    .orElse(null);

            if (existingContainer == null) {
                productDto.getContainers().add(contDto);
            } else {
                InterfaceDTOTree existingIface = existingContainer.getInterfaces().stream()
                        .filter(i -> i.getInterfaceId().equals(interf.getId()))
                        .findFirst()
                        .orElse(null);

                if (existingIface == null) {
                    existingContainer.getInterfaces().add(ifaceDto);
                } else {
                    existingIface.getOperations().add(opDto);
                }
            }
        }

        if (productMap.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(productMap.values());
    }
}
