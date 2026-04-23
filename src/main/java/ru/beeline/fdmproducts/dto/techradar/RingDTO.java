package ru.beeline.fdmproducts.dto.techradar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RingDTO {
    private Integer id;
    private String name;
    private Integer order;
}
