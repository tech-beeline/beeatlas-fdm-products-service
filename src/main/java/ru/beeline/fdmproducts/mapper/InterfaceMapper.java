package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.dto.InterfaceDTO;
import ru.beeline.fdmproducts.dto.SearchCapabilityDTO;

import java.util.Date;
import java.util.List;

@Component
public class InterfaceMapper {
    public Interface convertToInterface(InterfaceDTO interfaceDTO, Integer containerId, List<SearchCapabilityDTO> searchCapabilityDTOS) {
        return Interface.builder()
                .name(interfaceDTO.getName())
                .code(interfaceDTO.getCode())
                .version(interfaceDTO.getVersion())
                .specLink(interfaceDTO.getSpecLink())
                .protocol(interfaceDTO.getProtocol())
                .tcId(searchCapabilityDTOS.get(0).getId())
                .containerId(containerId)
                .createdDate(new Date())
                .build();
    }

    public void updateInterface(Interface interfaceEntity, InterfaceDTO interfaceDTO, Integer containerId, List<SearchCapabilityDTO> searchCapabilityDTOS) {
        interfaceEntity.setContainerId(containerId);
        interfaceEntity.setTcId(searchCapabilityDTOS.get(0).getId());
        interfaceEntity.setName(interfaceDTO.getName());
        interfaceEntity.setCode(interfaceDTO.getCode());
        interfaceEntity.setVersion(interfaceDTO.getVersion());
        interfaceEntity.setSpecLink(interfaceDTO.getSpecLink());
        interfaceEntity.setProtocol(interfaceDTO.getProtocol());
        interfaceEntity.setUpdatedDate(new Date());
        interfaceEntity.setDeletedDate(null);
    }
}

