/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.MapicOperationFullDTO;
import ru.beeline.fdmproducts.dto.MethodDTO;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DiscoveredOperationMapper {

    @Autowired
    DiscoveredInterfaceRepository discoveredInterfaceRepository;

    public List<MapicOperationFullDTO> createMapicOperationFullDTO(List<DiscoveredOperation> discoveredOperations) {
        List<MapicOperationFullDTO> result = new ArrayList<>();
        if (discoveredOperations != null && !discoveredOperations.isEmpty()) {
            for (DiscoveredOperation discoveredOperation : discoveredOperations) {
                {
                    String contextApi = null;
                    Optional<DiscoveredInterface> optDiscoveredInterface = discoveredInterfaceRepository.findById(
                            discoveredOperation.getInterfaceId());
                    if (optDiscoveredInterface.isPresent()) {
                        contextApi = optDiscoveredInterface.get().getContext();
                    }
                    result.add(MapicOperationFullDTO.builder()
                                       .id(discoveredOperation.getId())
                                       .name(discoveredOperation.getName())
                                       .type(discoveredOperation.getType())
                                       .description(discoveredOperation.getDescription())
                                       .context(discoveredOperation.getContext())
                                       .contextApi(contextApi)
                                       .build());
                }
            }
        }
        return result;
    }
}
