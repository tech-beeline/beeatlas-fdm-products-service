package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.MethodDTO;

import java.util.Date;

@Component
public class OperationMapper {
    public Operation convertToOperation(MethodDTO methodDTO, Integer interfaceId) {
        return Operation.builder()
                .name(methodDTO.getName())
                .description(methodDTO.getDescription())
                .returnType(methodDTO.getReturnType())
                .interfaceId(interfaceId)
                .createdDate(new Date())
                .build();
    }

    public void updateOperation(Operation operation, MethodDTO methodDTO) {
        operation.setName(methodDTO.getName());
        operation.setDescription(methodDTO.getDescription());
        operation.setReturnType(methodDTO.getReturnType());
        operation.setUpdatedDate(new Date());
    }
}
