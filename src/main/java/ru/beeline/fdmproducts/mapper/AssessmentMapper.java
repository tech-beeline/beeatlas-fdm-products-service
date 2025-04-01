package ru.beeline.fdmproducts.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.LocalAssessment;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.dto.AssessmentResponseDTO;

@Component
public class AssessmentMapper {
    @Autowired
    FitnessFunctionMapper fitnessFunctionMapper;

    public AssessmentResponseDTO mapToAssessmentResponseDTO(
            LocalAssessment assessment,
            Product product) {
        AssessmentResponseDTO response = new AssessmentResponseDTO();
        response.setAssessmentId(assessment.getId());
        response.setSourceId(assessment.getSourceId());
        response.setCreatedDate(assessment.getCreatedTime());
        response.setProductId(product.getId());
        response.setFitnessFunctions(assessment.getChecks().stream()
                .map(fitnessFunctionMapper::mapToFitnessFunctionResponse)
                .toList());

        return response;
    }
}
