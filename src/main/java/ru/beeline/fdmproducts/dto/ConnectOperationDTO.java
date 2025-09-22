package ru.beeline.fdmproducts.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ConnectOperationDTO {

    private Integer id;
    private String name;
    private String description;
    private String type;
    private MapicOperationDTO connectOperation;
}
