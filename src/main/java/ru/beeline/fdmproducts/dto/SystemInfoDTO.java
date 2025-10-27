package ru.beeline.fdmproducts.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class SystemInfoDTO {
    private String alias;
    private String description;
    private String gitUrl;
    private String id;
    private String name;
    private String structurizrApiUrl;
    private String structurizrWorkspaceName;
    private String uploadSource;
    private LocalDateTime uploadDate;
    private String critical;
    private String ownerName;
    private String ownerEmail;
}
