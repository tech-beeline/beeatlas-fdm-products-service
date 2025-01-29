package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.Parameter;
import ru.beeline.fdmproducts.dto.ParameterDTO;

import java.util.Date;

@Component
public class ParameterMapper {
    public Parameter convertToParameter(ParameterDTO parameterDTO, Integer operationId) {
        return Parameter.builder()
                .operationId(operationId)
                .parameterName(parameterDTO.getName())
                .parameterType(parameterDTO.getType())
                .createdDate(new Date())
                .build();
    }
}
