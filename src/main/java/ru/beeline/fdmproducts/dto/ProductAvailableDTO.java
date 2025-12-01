package ru.beeline.fdmproducts.dto;

import lombok.*;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.TechProduct;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductAvailableDTO {

    private Boolean availability;

}
