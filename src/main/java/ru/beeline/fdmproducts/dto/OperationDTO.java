package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class OperationDTO {

    private Integer id;
    private String name;
    private String description;
    private String type;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private List<MapicOperationDTO> mapicOperations;
}