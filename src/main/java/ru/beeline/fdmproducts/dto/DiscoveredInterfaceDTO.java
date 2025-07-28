package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class DiscoveredInterfaceDTO {

    private String name;
    private Integer externalId;
    private Integer apiId;
    private String apiLink;
    private String version;
    private String description;
    private String status;
    private String context;
    private Integer productId;
}
