package ru.beeline.fdmproducts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechCapabilityDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String author;
    private String link;
    private Date createdDate;
    private Date updatedDate;
    private Date deletedDate;
    private String owner;
}
