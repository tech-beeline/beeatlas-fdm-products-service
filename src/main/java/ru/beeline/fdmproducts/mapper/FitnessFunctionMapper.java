package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.LocalAcObject;
import ru.beeline.fdmproducts.domain.LocalAcObjectDetail;
import ru.beeline.fdmproducts.domain.LocalAssessmentCheck;
import ru.beeline.fdmproducts.dto.AssessmentObjectDTO;
import ru.beeline.fdmproducts.dto.DetailsDTO;
import ru.beeline.fdmproducts.dto.FitnessFunctionResponseDTO;
import ru.beeline.fdmproducts.repository.LocalAcObjectRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class FitnessFunctionMapper {

    private final LocalAcObjectRepository localAcObjectRepository;

    public FitnessFunctionMapper(LocalAcObjectRepository localAcObjectRepository) {
        this.localAcObjectRepository = localAcObjectRepository;
    }

    public FitnessFunctionResponseDTO mapToFitnessFunctionResponse(LocalAssessmentCheck check) {

        List<LocalAcObject> localAcObjectList = localAcObjectRepository.findAllByLacId(check.getId());
        Set<String> tableStruct = new HashSet<>();

        FitnessFunctionResponseDTO fitnessFunctionDTO = new FitnessFunctionResponseDTO();
        fitnessFunctionDTO.setId(check.getId());
        fitnessFunctionDTO.setCode(check.getFitnessFunction().getCode());
        fitnessFunctionDTO.setDescription(check.getFitnessFunction().getDescription());
        fitnessFunctionDTO.setIsCheck(check.getIsCheck());
        fitnessFunctionDTO.setResultDetails(check.getResultDetails());
        fitnessFunctionDTO.setStatus(check.getFitnessFunction().getStatus());
        fitnessFunctionDTO.setDocLink(check.getFitnessFunction().getDocLink());
        fitnessFunctionDTO.setAssessmentDescription(check.getAssessmentDescription());
        fitnessFunctionDTO.setDetails(mapAssessmentObjectDTO(localAcObjectList, tableStruct));
        fitnessFunctionDTO.setTableStruct(new ArrayList<>(tableStruct));

        return fitnessFunctionDTO;
    }

    private List<AssessmentObjectDTO> mapAssessmentObjectDTO(List<LocalAcObject> localAcObjectList, Set<String> tableStruct) {

        List<AssessmentObjectDTO> result = new ArrayList<>();
        for (LocalAcObject localAcObject : localAcObjectList) {
            result.add(AssessmentObjectDTO.builder()
                    .check(localAcObject.getIsCheck())
                    .details(mapDetailsDTO(localAcObject.getDetails(), tableStruct))
                    .build());
        }
        return result;
    }

    private List<DetailsDTO> mapDetailsDTO(List<LocalAcObjectDetail> details, Set<String> tableStruct) {

        List<DetailsDTO> result = new ArrayList<>();
        for (LocalAcObjectDetail localAcObj : details) {
            tableStruct.add(localAcObj.getKey());
            result.add(DetailsDTO.builder()
                    .key(localAcObj.getKey())
                    .value(localAcObj.getValue())
                    .build());
        }
        return result;
    }
}
