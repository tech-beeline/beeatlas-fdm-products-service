/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Строка product.sla канонической операции")
public class E2eV2ConnectedOperationSlaDTO {

    private Integer id;
    private Integer operationId;
    private Double rps;
    private Double latency;
    private Double errorRate;
}
