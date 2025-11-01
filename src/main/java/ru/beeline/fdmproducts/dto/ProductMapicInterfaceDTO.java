package ru.beeline.fdmproducts.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductMapicInterfaceDTO {

    private Integer id;
    private String name;
    private String version;
    private Integer externalId;
    private Integer apiId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime updateDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createDate;
    private LocalDateTime deletedDate;
    private String description;
    private String context;
    private String contextProvider;
    private MapicInterfaceDTO connectInterface;
    private List<ConnectOperationDTO> operations;
}