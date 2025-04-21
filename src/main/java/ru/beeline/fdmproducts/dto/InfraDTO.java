package ru.beeline.fdmproducts.dto;


import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class InfraDTO {
    private String name;
    private String type;
    private String cmdbId;
    private List<PropertyDTO> properties;
}
