/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.discovered;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UseCaseDiscoveredOperationDTO {

    private String name;
    private String type;
    private String descriptionTc;
    private String tcCode;
    private String descriptionOperation;
    private Boolean isPlanned;
    private Integer connectionOperationId;
}
