package ru.beeline.fdmproducts.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class InterfaceDTO {

    private String name;
    private String code;
    private String version;
    private String specLink;
    private String specification;
    private String capabilityCode;
    private String protocol;
    private List <MethodDTO> methods;
    private SlaDTO sla;
}
