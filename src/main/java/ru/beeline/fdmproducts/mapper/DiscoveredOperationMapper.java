/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.dto.ConnectionOperationDTO;
import ru.beeline.fdmproducts.dto.ContainerSearchDTO;
import ru.beeline.fdmproducts.dto.DiscoveredOperationDTO;
import ru.beeline.fdmproducts.dto.InterfaceSearchDTO;
import ru.beeline.fdmproducts.dto.MapicOperationFullDTO;
import ru.beeline.fdmproducts.dto.ProductSearchDTO;
import ru.beeline.fdmproducts.dto.search.projection.ArchOperationProjection;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DiscoveredOperationMapper {

    @Autowired
    DiscoveredInterfaceRepository discoveredInterfaceRepository;

    public List<MapicOperationFullDTO> createMapicOperationFullDTO(List<DiscoveredOperation> discoveredOperations) {
        List<MapicOperationFullDTO> result = new ArrayList<>();
        if (discoveredOperations != null && !discoveredOperations.isEmpty()) {
            for (DiscoveredOperation discoveredOperation : discoveredOperations) {
                {
                    String contextApi = null;
                    Optional<DiscoveredInterface> optDiscoveredInterface = discoveredInterfaceRepository.findById(
                            discoveredOperation.getInterfaceId());
                    if (optDiscoveredInterface.isPresent()) {
                        contextApi = optDiscoveredInterface.get().getContext();
                    }
                    result.add(MapicOperationFullDTO.builder()
                            .id(discoveredOperation.getId())
                            .name(discoveredOperation.getName())
                            .type(discoveredOperation.getType())
                            .description(discoveredOperation.getDescription())
                            .context(discoveredOperation.getContext())
                            .contextApi(contextApi)
                            .build());
                }
            }
        }
        return result;
    }

    public DiscoveredOperationDTO mapToOperationDTO(DiscoveredOperation discoveredOperation,
                                                    ArchOperationProjection projection) {
        return mapToOperationDTO(discoveredOperation, projection, Map.of());
    }

    public DiscoveredOperationDTO mapToOperationDTO(DiscoveredOperation discoveredOperation,
                                                    ArchOperationProjection mainProjection,
                                                    Map<Integer, ArchOperationProjection> projectionsById) {
        ConnectionOperationDTO connectionOperationDev = toConnectionOperation(
                projectionsById.get(discoveredOperation.getConnectionOperationDevId()));
        ConnectionOperationDTO connectionOperationDesign = toConnectionOperation(
                projectionsById.get(discoveredOperation.getConnectionOperationDesignId()));
        if (mainProjection == null) {
            return DiscoveredOperationDTO.builder()
                    .id(discoveredOperation.getId())
                    .name(discoveredOperation.getName())
                    .type(discoveredOperation.getType())
                    .connectionOperation(null)
                    .connectionOperationDev(connectionOperationDev)
                    .connectionOperationDesign(connectionOperationDesign)
                    .interfaceObj(null)
                    .container(null)
                    .product(null)
                    .build();
        }
        return DiscoveredOperationDTO.builder()
                .id(discoveredOperation.getId())
                .name(discoveredOperation.getName())
                .type(discoveredOperation.getType())
                .connectionOperation(toConnectionOperation(mainProjection))
                .connectionOperationDev(connectionOperationDev)
                .connectionOperationDesign(connectionOperationDesign)
                .interfaceObj(InterfaceSearchDTO.builder()
                        .id(mainProjection.getInterfaceId())
                        .code(mainProjection.getInterfaceCode())
                        .name(mainProjection.getInterfaceName())
                        .build())
                .container(ContainerSearchDTO.builder()
                        .id(mainProjection.getContainerId())
                        .name(mainProjection.getContainerName())
                        .code(mainProjection.getContainerCode())
                        .build())
                .product(ProductSearchDTO.builder()
                        .id(mainProjection.getProductId())
                        .name(mainProjection.getProductName())
                        .alias(mainProjection.getProductAlias())
                        .branchName(mainProjection.getProductBranchName())
                        .build())
                .build();
    }

    public static ConnectionOperationDTO toConnectionOperation(ArchOperationProjection projection) {
        if (projection == null) {
            return null;
        }
        return ConnectionOperationDTO.builder()
                .id(projection.getOpId())
                .name(projection.getOpName())
                .type(projection.getOpType())
                .build();
    }
}
