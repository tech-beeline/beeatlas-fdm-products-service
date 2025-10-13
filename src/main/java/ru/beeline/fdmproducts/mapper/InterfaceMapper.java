package ru.beeline.fdmproducts.mapper;

import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.*;

import java.time.LocalDateTime;

public class InterfaceMapper {

    public static Interface convertToInterface(InterfaceDTO interfaceDTO, Integer containerId, Integer tcId) {
        return Interface.builder()
                .name(interfaceDTO.getName())
                .code(interfaceDTO.getCode())
                .version(interfaceDTO.getVersion())
                .specLink(interfaceDTO.getSpecLink())
                .protocol(interfaceDTO.getProtocol())
                .tcId(tcId)
                .containerId(containerId)
                .createdDate(LocalDateTime.now())
                .build();
    }

    public static void updateInterface(Interface interfaceEntity,
                                InterfaceDTO interfaceDTO,
                                Integer containerId,
                                Integer tcId) {
        interfaceEntity.setContainerId(containerId);
        interfaceEntity.setTcId(tcId);
        interfaceEntity.setName(interfaceDTO.getName());
        interfaceEntity.setCode(interfaceDTO.getCode());
        interfaceEntity.setVersion(interfaceDTO.getVersion());
        interfaceEntity.setSpecLink(interfaceDTO.getSpecLink());
        interfaceEntity.setProtocol(interfaceDTO.getProtocol());
        interfaceEntity.setUpdatedDate(LocalDateTime.now());
    }

    public static MapicInterfaceDTO createMapicInterfaceDTO(DiscoveredInterface dInterface) {
        return MapicInterfaceDTO.builder()
                .id(dInterface.getId())
                .name(dInterface.getName())
                .description(dInterface.getDescription())
                .build();
    }

    public static MapicInterfaceDTO createMapicInterfaceDTO(DiscoveredInterface dInterface, Interface anInterface) {
        return MapicInterfaceDTO.builder()
                .id(dInterface.getConnectionInterfaceId())
                .name(anInterface != null ? anInterface.getName() : null)
                .description(anInterface != null ? anInterface.getDescription() : null)
                .build();
    }

    public static ProductInterfaceDTO createProductInterface(Interface interfaceObj) {
        return ProductInterfaceDTO.builder()
                .id(interfaceObj.getId())
                .name(interfaceObj.getName())
                .version(interfaceObj.getVersion())
                .description(interfaceObj.getDescription())
                .code(interfaceObj.getCode())
                .createDate(interfaceObj.getCreatedDate())
                .updateDate(interfaceObj.getUpdatedDate())
                .build();
    }

    public static ProductMapicInterfaceDTO createProductMapicInterface(DiscoveredInterface interfaceObj) {
        return ProductMapicInterfaceDTO.builder()
                .id(interfaceObj.getId())
                .name(interfaceObj.getName())
                .version(interfaceObj.getVersion())
                .description(interfaceObj.getDescription())
                .externalId(interfaceObj.getExternalId())
                .createDate(interfaceObj.getCreatedDate())
                .updateDate(interfaceObj.getUpdatedDate())
                .apiId(interfaceObj.getApiId())
                .context(interfaceObj.getContext())
                .build();
    }

    public static ConnectOperationDTO createConnectOperationDTO(Operation operation,
                                                          DiscoveredOperation discoveredOperation) {
        ConnectOperationDTO connectOperationDTO = ConnectOperationDTO.builder()
                .id(discoveredOperation.getId())
                .name(discoveredOperation.getName())
                .description(discoveredOperation.getDescription())
                .createDate(discoveredOperation.getCreatedDate())
                .updateDate(discoveredOperation.getUpdatedDate())
                .type(discoveredOperation.getType())
                .build();
        if (operation != null) {
            MapicOperationDTO mapicOperationDTO = MapicOperationDTO.builder()
                    .id(discoveredOperation.getConnectionOperationId())
                    .name(operation.getName())
                    .description(operation.getDescription())
                    .type(operation.getType())
                    .build();
            connectOperationDTO.setConnectOperation(mapicOperationDTO);
        }
        return connectOperationDTO;
    }

}

