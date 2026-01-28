/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class OperationE2eDTO {

    private String name;
    private String uid;
    @JsonProperty("interface")
    private InterfaceE2eDTO interfaceE2eDTO;
}
