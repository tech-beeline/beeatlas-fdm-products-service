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
public class InterfaceMethodDTO {

    private Integer id;
    private String description;
    private String name;
    private String specLink;
    private String protocol;
    private String version;
    private TcDTO techCapability;
    private String code;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private List<MapicInterfaceDTO> mapicInterfaces;
    private List<OperationFullDTO> operations;
}
