package ru.beeline.fdmproducts.dto;

import lombok.*;

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
    private List<InterfaceMethodDTO> interfaces;
}
