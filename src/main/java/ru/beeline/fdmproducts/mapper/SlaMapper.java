/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.mapper;

import org.springframework.stereotype.Component;
import ru.beeline.fdmproducts.domain.Sla;
import ru.beeline.fdmproducts.dto.MethodDTO;

@Component
public class SlaMapper {
    public Sla convertToSla(MethodDTO methodDTO, Integer operationId) {
        return Sla.builder()
                .operationId(operationId)
                .rps(methodDTO.getSla().getRps())
                .latency(methodDTO.getSla().getLatency())
                .errorRate(methodDTO.getSla().getErrorRate())
                .build();
    }

    public void updateSla(Sla sla, MethodDTO methodDTO) {
        sla.setRps(methodDTO.getSla().getRps());
        sla.setLatency(methodDTO.getSla().getLatency());
        sla.setErrorRate(methodDTO.getSla().getErrorRate());
    }
}