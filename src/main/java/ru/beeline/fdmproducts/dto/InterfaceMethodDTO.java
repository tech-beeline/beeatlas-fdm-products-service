/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import ru.beeline.fdmproducts.dto.TcDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class InterfaceMethodDTO {

    private Integer id;
    private String description;
    private String name;
    private String specLink;
    private String protocol;
    private String version;
    private TcDTO techCapability;
    private String code;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deletedDate;
    private List<MapicInterfaceDTO> mapicInterfaces;
    private List<OperationFullDTO> operations;
}
