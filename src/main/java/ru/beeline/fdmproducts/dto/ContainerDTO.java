package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ContainerDTO {

    private String name;
    private String code;
    private String version;
    private List <InterfaceDTO> interfaces;
}
