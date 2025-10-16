package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.MapicOperationDTO;
import ru.beeline.fdmproducts.dto.MethodDTO;
import ru.beeline.fdmproducts.dto.OperationDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class OperationMapper {
    public Operation convertToOperation(MethodDTO methodDTO, Integer interfaceId, Integer tcId) {
        return Operation.builder()
                .tcId(tcId)
                .type(methodDTO.getType())
                .name(methodDTO.getName())
                .description(methodDTO.getDescription())
                .returnType(methodDTO.getReturnType())
                .interfaceId(interfaceId)
                .createdDate(LocalDateTime.now())
                .build();
    }

    public void updateOperation(Operation operation, MethodDTO methodDTO, Integer tcId, Integer interfaceId) {
        operation.setTcId(tcId);
        operation.setName(methodDTO.getName());
        operation.setDescription(methodDTO.getDescription());
        operation.setInterfaceId(interfaceId);
        operation.setReturnType(methodDTO.getReturnType());
        operation.setType(methodDTO.getType());
        operation.setUpdatedDate(LocalDateTime.now());
    }

    public OperationDTO createOperationDTO(Operation operation, List<DiscoveredOperation> discoveredOperations) {
        OperationDTO operationDTO = OperationDTO.builder()
                .id(operation.getId())
                .name(operation.getName())
                .description(operation.getDescription())
                .type(operation.getType())
                .createDate(operation.getCreatedDate())
                .updateDate(operation.getUpdatedDate())
                .build();
        List<MapicOperationDTO> result = new ArrayList<>();
        if (discoveredOperations != null && !discoveredOperations.isEmpty()) {
            for (DiscoveredOperation dOperation : discoveredOperations) {
                result.add(MapicOperationDTO.builder()
                                   .id(dOperation.getId())
                                   .name(dOperation.getName())
                                   .description(dOperation.getDescription())
                                   .type(dOperation.getType())
                                   .build());
            }
        }
        operationDTO.setMapicOperations(result);
        return operationDTO;
    }
}
