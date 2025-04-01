package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.LocalAssessmentCheck;
import ru.beeline.fdmproducts.domain.LocalFitnessFunction;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.ContainerDTO;
import ru.beeline.fdmproducts.dto.FitnessFunctionResponseDTO;

import java.util.Date;

@Component
public class FitnessFunctionMapper {

    public FitnessFunctionResponseDTO mapToFitnessFunctionResponse(
            LocalAssessmentCheck check) {

        FitnessFunctionResponseDTO fitnessFunctionDTO = new FitnessFunctionResponseDTO();
        fitnessFunctionDTO.setId(check.getId());
        fitnessFunctionDTO.setCode(check.getFitnessFunction().getCode());
        fitnessFunctionDTO.setDescription(check.getFitnessFunction().getDescription());
        fitnessFunctionDTO.setIsCheck(check.getIsCheck());
        fitnessFunctionDTO.setResultDetails(check.getResultDetails());
        fitnessFunctionDTO.setStatus(check.getFitnessFunction().getStatus());

        return fitnessFunctionDTO;
    }
}
