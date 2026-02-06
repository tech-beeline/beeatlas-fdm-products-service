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
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.mapper.ArchOperationMapper;
import ru.beeline.fdmproducts.mapper.DiscoveredOperationMapper;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;
import ru.beeline.fdmproducts.repository.OperationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                .collect(Collectors.toMap(
                        ArchOperationProjection::getOpId,
                        Function.identity()
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

    public List<ArchOperationDTO> getOperationByTc(Integer tcId) {
        List<ArchOperationDTO> result = new ArrayList<>();
        List<Operation> operations = operationRepository.findOperationsWithFullChainGraph(tcId);
        if (!operations.isEmpty()) {
            result = operations.stream().map(operation -> archOperationMapper.mapToArchOperationDTO(operation))
                    .toList();
        }
        return result;
    }
}
