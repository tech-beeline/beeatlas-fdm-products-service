/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.dto.OperationTechCapabilityDTO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductE2eOperationDTO {
    private Integer id;
    private String entityType;
    private String type;
    private String name;
    private OperationTechCapabilityDTO techCapability;
}
