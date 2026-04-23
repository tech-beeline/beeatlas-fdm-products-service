package ru.beeline.fdmproducts.dto;

import lombok.*;
import ru.beeline.fdmproducts.dto.ffunction.FitnessFunctionProductDTO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DomainDTO {
    private Integer id;
    private String name;
    private String alias;
    private Integer ownerId;
    private List<FitnessFunctionProductDTO> product;
}