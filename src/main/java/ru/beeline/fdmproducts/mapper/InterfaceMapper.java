package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.Interface;
import ru.beeline.fdmproducts.dto.InterfaceDTO;

import java.util.Date;

@Component
public class InterfaceMapper {
    public Interface convertToInterface(InterfaceDTO interfaceDTO, Integer containerId, Integer tcId) {
        return Interface.builder()
                .name(interfaceDTO.getName())
                .code(interfaceDTO.getCode())
                .version(interfaceDTO.getVersion())
                .specLink(interfaceDTO.getSpecLink())
                .protocol(interfaceDTO.getProtocol())
                .tcId(tcId)
                .containerId(containerId)
                .createdDate(new Date())
                .build();
    }

    public void updateInterface(Interface interfaceEntity, InterfaceDTO interfaceDTO, Integer containerId, Integer tcId) {
        interfaceEntity.setContainerId(containerId);
        interfaceEntity.setTcId(tcId);
        interfaceEntity.setName(interfaceDTO.getName());
        interfaceEntity.setCode(interfaceDTO.getCode());
        interfaceEntity.setVersion(interfaceDTO.getVersion());
        interfaceEntity.setSpecLink(interfaceDTO.getSpecLink());
        interfaceEntity.setProtocol(interfaceDTO.getProtocol());
        interfaceEntity.setUpdatedDate(new Date());
    }
}

