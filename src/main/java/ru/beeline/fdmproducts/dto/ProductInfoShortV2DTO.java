package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductInfoShortV2DTO {

    private String alias;
    private String description;
    private String gitUrl;
    private String id;
    private String name;
    private String structurizrApiUrl;
    private String structurizrWorkspaceName;

}
