package ru.beeline.fdmproducts.dto.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class DiscoveredOperationDTO {

    private Integer id;
    private String name;
    private String type;
    private ConnectionOperationDTO connectionOperation;
    @JsonProperty("interface")
    private InterfaceSearchDTO interfaceObj;
    private ContainerSearchDTO container;
    private ProductSearchDTO product;
}
