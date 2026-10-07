/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.dto.discovered.OperationWithTechCapabilityDTO;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Discovered-метод, участвующий в шагах e2e, с вложенной канонической операцией")
public class E2eGroupedByE2eOperationDTO {

    private Integer id;
    private Integer interfaceId;
    private Integer connectionOperationId;
    private String name;
    private String context;
    private String description;
    private String tcCode;
    private String tcDescription;
    private String type;
    private String returnType;
    private Double rps;
    private Double latency;
    private Double errorRate;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime deletedDate;
    private OperationWithTechCapabilityDTO archOperation;
}
