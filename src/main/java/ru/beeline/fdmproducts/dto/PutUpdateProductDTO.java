package ru.beeline.fdmproducts.dto;


import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class PutUpdateProductDTO {

    private String alias;
    private String description;
    private String gitUrl;
    private String name;
    private Integer ownerId;
    private String critical;
    private List<Integer> employeesIds;
}
