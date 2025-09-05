package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductInfoShortDTO {

    private String alias;
    private String description;
    private String gitUrl;
    private String id;
    private String name;
    private String structurizrApiUrl;
    private String structurizrWorkspaceName;

}
