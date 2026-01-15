package ru.beeline.fdmproducts.dto.search;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class InterfaceSearchDTO {

    private Integer id;
    private String name;
    private String code;
}
