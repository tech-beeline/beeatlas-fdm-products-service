package ru.beeline.fdmproducts.dto.search;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class OperationSearchDTO {

    private List<ArchOperationDTO> archOperations;
    private List<DiscoveredOperationDTO> discoveredOperations;
}
