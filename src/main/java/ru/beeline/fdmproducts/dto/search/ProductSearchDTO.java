package ru.beeline.fdmproducts.dto.search;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductSearchDTO {

    private Integer id;
    private String name;
    private String alias;
}
