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
public class OperationFullDTO {

    private Integer id;
    private String name;
    private String description;
    private String type;
    private List<MapicOperationFullDTO> mapicOperations;
    private TcDTO techCapability;
    private SlaV2DTO sla;
    private LocalDateTime createdDate;
    private LocalDateTime updateDate;
}
