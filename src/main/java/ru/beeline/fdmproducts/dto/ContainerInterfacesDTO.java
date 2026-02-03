/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.Date;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ContainerInterfacesDTO {

    private Integer id;
    private String name;
    private String code;
    private Date createDate;
    private Date updateDate;
    private Date deletedDate;
    private List<InterfaceMethodDTO> interfaces;
}
