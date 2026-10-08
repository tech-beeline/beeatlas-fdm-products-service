/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.ContainerProduct;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.domain.DiscoveredOperation;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.domain.Operation;
import ru.beeline.fdmproducts.domain.Product;
import ru.beeline.fdmproducts.domain.Sla;
import ru.beeline.fdmproducts.dto.TcDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredConnectedOperationDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredInterfaceByIdsDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredInterfaceProductDTO;
import ru.beeline.fdmproducts.dto.discovered.DiscoveredOperationByIdsDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2ConnectedContainerDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2ConnectedInterfaceDTO;
import ru.beeline.fdmproducts.dto.e2e.E2eV2ConnectedOperationSlaDTO;

@Component
public class DiscoveredOperationByIdsMapper {

    public DiscoveredOperationByIdsDTO toDto(DiscoveredOperation discoveredOperation,
                                             DiscoveredInterface discoveredInterface,
                                             DiscoveredConnectedOperationDTO operation) {
        return DiscoveredOperationByIdsDTO.builder()
                .id(discoveredOperation.getId())
                .interfaceId(discoveredOperation.getInterfaceId())
                .connectionOperationId(discoveredOperation.getConnectionOperationId())
                .name(discoveredOperation.getName())
                .context(discoveredOperation.getContext())
                .description(discoveredOperation.getDescription())
                .tcCode(discoveredOperation.getTcCode())
                .tcDescription(discoveredOperation.getTcDescription())
                .isPlanned(discoveredOperation.getIsPlanned())
                .type(discoveredOperation.getType())
                .returnType(discoveredOperation.getReturnType())
                .rps(discoveredOperation.getRps())
                .latency(discoveredOperation.getLatency())
                .errorRate(discoveredOperation.getErrorRate())
                .createdDate(discoveredOperation.getCreatedDate())
                .updatedDate(discoveredOperation.getUpdatedDate())
                .deletedDate(discoveredOperation.getDeletedDate())
                .discoveredInterface(toInterfaceDto(discoveredInterface))
                .operation(operation)
                .build();
    }

    public DiscoveredConnectedOperationDTO toConnectedOperationDto(Operation operation,
                                                                   TcDTO techCapability,
                                                                   Sla sla,
                                                                   Interface iface,
                                                                   ContainerProduct container) {
        if (operation == null) {
            return null;
        }
        return DiscoveredConnectedOperationDTO.builder()
                .id(operation.getId())
                .name(operation.getName())
                .type(operation.getType())
                .tcId(operation.getTcId())
                .techCapability(techCapability)
                .sla(toSlaDto(sla))
                .connectedInterface(toConnectedInterfaceDto(iface, container))
                .build();
    }

    private DiscoveredInterfaceByIdsDTO toInterfaceDto(DiscoveredInterface discoveredInterface) {
        if (discoveredInterface == null) {
            return null;
        }
        Product product = discoveredInterface.getProduct();
        return DiscoveredInterfaceByIdsDTO.builder()
                .id(discoveredInterface.getId())
                .name(discoveredInterface.getName())
                .externalId(discoveredInterface.getExternalId())
                .apiId(discoveredInterface.getApiId())
                .connectionInterfaceId(discoveredInterface.getConnectionInterfaceId())
                .apiLink(discoveredInterface.getApiLink())
                .version(discoveredInterface.getVersion())
                .description(discoveredInterface.getDescription())
                .status(discoveredInterface.getStatus())
                .context(discoveredInterface.getContext())
                .source(discoveredInterface.getSource())
                .productId(product == null ? null : product.getId())
                .product(toProductDto(product))
                .productPlanned(discoveredInterface.getProductPlanned())
                .createdDate(discoveredInterface.getCreatedDate())
                .updatedDate(discoveredInterface.getUpdatedDate())
                .deletedDate(discoveredInterface.getDeletedDate())
                .build();
    }

    private DiscoveredInterfaceProductDTO toProductDto(Product product) {
        if (product == null) {
            return null;
        }
        return DiscoveredInterfaceProductDTO.builder()
                .id(product.getId())
                .alias(product.getAlias())
                .name(product.getName())
                .description(product.getDescription())
                .build();
    }

    private E2eV2ConnectedOperationSlaDTO toSlaDto(Sla sla) {
        if (sla == null) {
            return null;
        }
        return E2eV2ConnectedOperationSlaDTO.builder()
                .id(sla.getId())
                .operationId(sla.getOperationId())
                .rps(sla.getRps())
                .latency(sla.getLatency())
                .errorRate(sla.getErrorRate())
                .build();
    }

    private E2eV2ConnectedInterfaceDTO toConnectedInterfaceDto(Interface iface, ContainerProduct container) {
        if (iface == null) {
            return null;
        }
        E2eV2ConnectedContainerDTO containerDto = null;
        if (container != null) {
            containerDto = E2eV2ConnectedContainerDTO.builder()
                    .id(container.getId())
                    .code(container.getCode())
                    .build();
        }
        return E2eV2ConnectedInterfaceDTO.builder()
                .id(iface.getId())
                .code(iface.getCode())
                .container(containerDto)
                .build();
    }
}
