package ru.beeline.fdmproducts.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import ru.beeline.fdmproducts.dto.techradar.TechAdvancedGetDTO;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * One element of the «techProducts» array in the V‑2 response.
 */
@Data
@Builder
public class TechProductInfoV2DTO {
    private Integer id;;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
    private LocalDateTime deletedDate;
    private String source;
    private TechAdvancedGetDTO tech;
}