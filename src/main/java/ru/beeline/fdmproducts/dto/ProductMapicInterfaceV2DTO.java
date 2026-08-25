/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Same shape as {@link ProductMapicInterfaceDTO}, except externalId is String — discovered_interface.external_id
 * is TEXT (V0043, to fit Sparx's compound interface codes alongside MAPIC's numeric ids), so forcing it into
 * Integer here throws NumberFormatException for any Sparx-sourced interface. v1 is left as-is to avoid a
 * breaking response-type change for existing callers; new/updated callers should use this v2 shape instead.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductMapicInterfaceV2DTO {

    private Integer id;
    private String name;
    private String version;
    private String externalId;
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
