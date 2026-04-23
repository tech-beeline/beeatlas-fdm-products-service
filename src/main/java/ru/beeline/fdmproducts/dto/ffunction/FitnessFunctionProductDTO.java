package ru.beeline.fdmproducts.dto.ffunction;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FitnessFunctionProductDTO {
    private Integer id;
    private String name;
    private String alias;
    private Integer ownerId;
    private List<FitnessFunctionShortDTO> fitnessFunctions;
}