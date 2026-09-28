/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.discovered;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UseCaseDiscoveredInterfaceResultDTO {

    private String interfaceCode;
    private String product;
    private String productPlanned;
    private List<UseCaseDiscoveredOperationResultDTO> operations;
}
