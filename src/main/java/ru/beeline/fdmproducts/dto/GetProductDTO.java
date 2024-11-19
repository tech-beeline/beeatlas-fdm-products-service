package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class GetProductDTO {

    private Integer id;
    private String name;
    private String alias;
}
