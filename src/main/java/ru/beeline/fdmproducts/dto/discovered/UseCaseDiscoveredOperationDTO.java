/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.discovered;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.fdmproducts.jackson.ConnectionOperationIdDeserializer;

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
    @JsonDeserialize(using = ConnectionOperationIdDeserializer.class)
    private Integer connectionOperationId;
}
