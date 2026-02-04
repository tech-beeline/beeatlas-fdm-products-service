/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.dto.*;
import ru.beeline.fdmlib.dto.product.TcDTO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static List<MapicInterfaceDTO> createMapicInterfaceList(List<DiscoveredInterface> discoveredInterfaces) {
        if (discoveredInterfaces == null || discoveredInterfaces.isEmpty()) {
            return Collections.emptyList();
        }
        return discoveredInterfaces.parallelStream()
                .map(InterfaceMapper::createMapicInterfaceDTO)
                .collect(Collectors.toList());
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
                .deletedDate(interfaceObj.getDeletedDate())
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
                .deletedDate(discoveredOperation.getDeletedDate())
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

    public static InterfaceMethodDTO createInterfaceMethodDTO(Interface interfaceObj, List<OperationFullDTO> operations,
                                                              List<MapicInterfaceDTO> mapicInterfaces, TcDTO techCapability) {
        return InterfaceMethodDTO.builder()
                .id(interfaceObj.getId())
                .name(interfaceObj.getName())
                .specLink(interfaceObj.getSpecLink())
                .protocol(interfaceObj.getProtocol())
                .description(interfaceObj.getDescription())
                .version(interfaceObj.getVersion())
                .code(interfaceObj.getCode())
                .createDate(interfaceObj.getCreatedDate())
                .updateDate(interfaceObj.getUpdatedDate())
                .deletedDate(interfaceObj.getDeletedDate())
                .mapicInterfaces(mapicInterfaces)
                .operations(operations)
                .techCapability(techCapability)
                .build();
    }

    public static List<InterfaceMethodDTO> createInterfaceMethodDTOList(List<Interface> interfaces,
                                                                        Map<Integer, List<OperationFullDTO>> operationsDTOByInterfaceId,
                                                                        Map<Integer, List<DiscoveredInterface>> discoveredInterfaceMap,
                                                                        Map<Integer, TcDTO> tcDTOMap) {
        return interfaces.parallelStream()
                .map(interfaceObj -> createInterfaceMethodDTO(
                        interfaceObj,
                        operationsDTOByInterfaceId.getOrDefault(interfaceObj.getId(), Collections.emptyList()),
                        createMapicInterfaceList(discoveredInterfaceMap.get(interfaceObj.getId())),
                        tcDTOMap.get(interfaceObj.getTcId())
                ))
                .collect(Collectors.toList());
    }
}

