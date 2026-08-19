/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.dto.SlaV2DTO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class E2eV2DiscoveredOperationCatalogItemDTO {

    private Integer id;
    private String name;
    private String type;
    private String interfaceCode;
    private String productAlias;
    private String source;
    private SlaV2DTO sla;
}
