/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.E2e;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.discovered.OperationWithTechCapabilityDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eOperationDTO;

import java.util.List;
import java.util.Map;

@Component
public class E2eGroupedByE2eMapper {

    public E2eGroupedByE2eDTO toE2eDto(E2e e2e, List<E2eGroupedByE2eOperationDTO> operations, String ownerAlias) {
        return E2eGroupedByE2eDTO.builder()
                .id(e2e.getId())
                .name(e2e.getName())
                .code(e2e.getCode())
                .source(e2e.getSource())
                .productId(e2e.getProductId())
                .productAlias(e2e.getProductId() == null ? null : ownerAlias)
                .operations(operations)
                .build();
    }

    public E2eGroupedByE2eOperationDTO toOperationDto(DiscoveredOperation operation,
                                                      Map<Integer, Operation> archOperations,
                                                      Map<Integer, TcDTO> techCapabilities) {
        OperationWithTechCapabilityDTO archOperation = null;
        if (operation.getConnectionOperationId() != null) {
            Operation connected = archOperations.get(operation.getConnectionOperationId());
            if (connected != null) {
                TcDTO techCapability = connected.getTcId() == null
                        ? null
                        : techCapabilities.get(connected.getTcId());
                archOperation = toArchOperationDto(connected, techCapability);
            }
        }
        return E2eGroupedByE2eOperationDTO.builder()
                .id(operation.getId())
                .interfaceId(operation.getInterfaceId())
                .connectionOperationId(operation.getConnectionOperationId())
                .name(operation.getName())
                .context(operation.getContext())
                .description(operation.getDescription())
                .tcCode(operation.getTcCode())
                .tcDescription(operation.getTcDescription())
                .type(operation.getType())
                .returnType(operation.getReturnType())
                .rps(operation.getRps())
                .latency(operation.getLatency())
                .errorRate(operation.getErrorRate())
                .createdDate(operation.getCreatedDate())
                .updatedDate(operation.getUpdatedDate())
                .deletedDate(operation.getDeletedDate())
                .archOperation(archOperation)
                .build();
    }

    private OperationWithTechCapabilityDTO toArchOperationDto(Operation operation, TcDTO techCapability) {
        return OperationWithTechCapabilityDTO.builder()
                .id(operation.getId())
                .interfaceId(operation.getInterfaceId())
                .name(operation.getName())
                .description(operation.getDescription())
                .type(operation.getType())
                .returnType(operation.getReturnType())
                .tcId(operation.getTcId())
                .isDeletedTc(operation.getIsDeletedTc())
                .createdDate(operation.getCreatedDate())
                .updatedDate(operation.getUpdatedDate())
                .deletedDate(operation.getDeletedDate())
                .techCapability(techCapability)
                .build();
    }
}
