package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductInterfaceDTO {

    private Integer id;
    private String name;
    private String version;
    private String description;
    private String code;
    private Date createDate;
    private Date updateDate;
    private List<MapicInterfaceDTO> mapicInterfaces;
    private List<OperationDTO> operations;
}