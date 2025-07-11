package ru.beeline.fdmproducts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RingDTO {

    private Integer id;
    private String name;
    private Integer order;
}
