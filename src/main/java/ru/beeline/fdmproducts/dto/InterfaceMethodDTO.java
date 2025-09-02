package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class InterfaceMethodDTO {

    private Integer id;
    private String description;
    private String name;
    private String version;
    private TcDTO techCapability;
    private MapicInterfaceDTO mapicInterface;
    private List<OperationFullDTO> operations;
}
