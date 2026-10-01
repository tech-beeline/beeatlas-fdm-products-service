/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.discovered;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.dto.TcDTO;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Каноническая операция product.operation и вложенная ТС")
public class OperationWithTechCapabilityDTO {

    private Integer id;
    private Integer interfaceId;
    private String name;
    private String description;
    private String type;
    private String returnType;
    private Integer tcId;
    private Boolean isDeletedTc;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime deletedDate;
    private TcDTO techCapability;
}
