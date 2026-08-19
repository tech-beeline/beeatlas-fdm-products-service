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
public class E2eV2InterfaceDTO {

    private String code;
    private String name;
    private String parentProductCmdb;
    private String specLink;
    private String version;
    private String protocol;
}
