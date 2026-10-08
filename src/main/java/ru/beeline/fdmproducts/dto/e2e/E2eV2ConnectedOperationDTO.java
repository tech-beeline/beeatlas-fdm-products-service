/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Каноническая операция, сопоставленная discovered_operation через connection_operation_id")
public class E2eV2ConnectedOperationDTO {

    private Integer id;
    private String name;
    private String type;
    private Integer tcId;
    private E2eV2ConnectedOperationSlaDTO sla;
    @JsonProperty("interface")
    private E2eV2ConnectedInterfaceDTO connectedInterface;
}
