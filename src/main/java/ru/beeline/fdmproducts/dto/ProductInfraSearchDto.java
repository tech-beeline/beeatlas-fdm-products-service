package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductInfraSearchDto {
    private String name;
    private String parameter;
    private String value;
    private List<String> parentSystems;
}
