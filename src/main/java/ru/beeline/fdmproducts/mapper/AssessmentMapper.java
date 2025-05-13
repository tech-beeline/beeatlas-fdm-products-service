package ru.beeline.fdmproducts.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.LocalAssessment;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.AssessmentResponseDTO;
import ru.beeline.fdmproducts.dto.SourceDTO;

@Component
public class AssessmentMapper {
    @Autowired
    FitnessFunctionMapper fitnessFunctionMapper;

    public AssessmentResponseDTO mapToAssessmentResponseDTO(LocalAssessment assessment, Product product, String sourceType) {
        AssessmentResponseDTO response = new AssessmentResponseDTO();
        response.setAssessmentId(assessment.getId());
        response.setSource(SourceDTO.builder()
                .sourceId(assessment.getSourceId())
                .sourceType(sourceType)
                .build());
        response.setCreatedDate(assessment.getCreatedTime());
        response.setProductId(product.getId());
        response.setFitnessFunctions(assessment.getChecks().stream()
                .map(fitnessFunctionMapper::mapToFitnessFunctionResponse)
                .toList());

        return response;
    }
}
