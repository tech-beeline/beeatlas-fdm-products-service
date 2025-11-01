package ru.beeline.fdmproducts.dto;

import lombok.*;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.TechProduct;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductFullDTO {

    private Integer id;
    private String alias;
    private String critical;
    private String description;
    private List<DiscoveredInterface> discoveredInterfaces;
    private String gitUrl;
    private String name;
    private Integer ownerID;
    private String ownerName;
    private String ownerEmail;
    private String source;
    private String structurizrApiKey;
    private String structurizrApiSecret;
    private String structurizrApiUrl;
    private String structurizrWorkspaceName;
    private List<TechProduct> techProducts;
    private LocalDateTime uploadDate;
}
