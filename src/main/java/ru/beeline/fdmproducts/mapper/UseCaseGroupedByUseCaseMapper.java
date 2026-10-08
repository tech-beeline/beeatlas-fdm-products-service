/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.dto.e2e.E2eGroupedByE2eOperationDTO;
import ru.beeline.fdmproducts.dto.project.UseCaseOperationsGroupedDTO;
import ru.beeline.fdmproducts.dto.usecase.UseCaseGroupedByUseCaseDTO;

import java.util.List;

@Component
public class UseCaseGroupedByUseCaseMapper {

    public UseCaseGroupedByUseCaseDTO toUseCaseDto(UseCaseOperationsGroupedDTO card,
                                                   List<E2eGroupedByE2eOperationDTO> operations) {
        return UseCaseGroupedByUseCaseDTO.builder()
                .id(card.getId())
                .name(card.getName())
                .code(card.getCode())
                .operations(operations)
                .build();
    }
}
