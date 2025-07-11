package ru.beeline.fdmproducts.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TechnologyDTO {

    private Integer id;
    private String label;
    private RingDTO ring;
    private SectorDTO sector;
}
