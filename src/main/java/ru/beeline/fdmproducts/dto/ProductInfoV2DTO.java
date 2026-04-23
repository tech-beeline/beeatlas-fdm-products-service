package ru.beeline.fdmproducts.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Whole payload for **GET /productInfoByCodeV2**.
 */
@Data
@Builder
public class ProductInfoV2DTO {
    private String alias;
    private String critical;
    private String description;
    private String gitUrl;
    private String id;
    private String name;
    private String ownerEmail;
    private String ownerName;
    private String structurizrApiUrl;
    private String structurizrWorkspaceName;
    private List<TechProductInfoV2DTO> techProducts;
}