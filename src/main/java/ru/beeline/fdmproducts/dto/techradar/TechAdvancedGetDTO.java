package ru.beeline.fdmproducts.dto.techradar;

import lombok.Data;

import java.util.List;

@Data
public class TechAdvancedGetDTO {
    private Integer id;
    private String label;
    private Boolean review;
    private String description;
    private String link;
    private String createdDate;
    private String deletedDate;
    private String lastModifiedDate;
    private RingDTO ring;
    private RingDTO sector;
    private Boolean isCritical;
    private List<TechCategoryAdvancedDTO> category;
}
