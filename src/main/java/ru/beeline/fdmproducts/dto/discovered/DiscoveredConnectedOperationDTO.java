/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.discovered;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2ConnectedInterfaceDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2ConnectedOperationSlaDTO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Каноническая operation: id, name, type, tcId, techCapability, sla, interface")
public class DiscoveredConnectedOperationDTO {

    private Integer id;
    private String name;
    private String type;
    private Integer tcId;
    private TcDTO techCapability;
    private E2eV2ConnectedOperationSlaDTO sla;
    @JsonProperty("interface")
    private E2eV2ConnectedInterfaceDTO connectedInterface;
}
