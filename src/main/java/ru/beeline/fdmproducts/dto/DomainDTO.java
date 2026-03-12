package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DomainDTO {
    private Long id;
    private String name;
    private String alias;
    private Long ownerId;
    private List<FitnessFunctionProductDTO> product;
}