package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductMapicInterfaceDTO {

    private Integer id;
    private String name;
    private String version;
    private Integer externalId;
    private Integer apiId;
    private String description;
    private String context;
    private String contextProvider;
    private MapicInterfaceDTO connectInterface;
    private List<ConnectOperationDTO> operations;
}