/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto.e2e;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class E2eInterfaceDTO {

    private String code;
    private String name;
    private String parentContainerCode;
    private String specLink;
    private String version;
    private String protocol;
    private Long containerVersionId;
    private Long interfaceVersionId;
}
