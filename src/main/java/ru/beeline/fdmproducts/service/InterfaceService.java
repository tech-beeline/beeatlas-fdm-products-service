/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.DiscoveredInterface;
import ru.beeline.fdmproducts.dto.ConnectionRequestDTO;
import ru.beeline.fdmproducts.repository.DiscoveredInterfaceRepository;
import ru.beeline.fdmproducts.repository.DiscoveredOperationRepository;
import ru.beeline.fdmproducts.repository.InterfaceRepository;

import java.time.LocalDateTime;

@Transactional
@Service
@Slf4j
public class InterfaceService {
    @Autowired
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Autowired
    private InterfaceRepository interfaceRepository;
    @Autowired
    private DiscoveredOperationRepository discoveredOperationRepository;


    public void handConnection(ConnectionRequestDTO request) {

        Integer mapicInterfaceId = request.getMapicInterfaceId();
        Integer archInterfaceId = request.getArchInterfaceId();

        DiscoveredInterface discoveredInterface = discoveredInterfaceRepository.findById(mapicInterfaceId)
                .orElseThrow(() -> new IllegalArgumentException("DiscoveredInterface отсутствует в БД"));

        LocalDateTime now = LocalDateTime.now();
        discoveredInterface.setConnectionInterfaceId(archInterfaceId);

        if (archInterfaceId != null) {
            interfaceRepository.findById(archInterfaceId)
                    .orElseThrow(() -> new IllegalArgumentException("Interface отсутствует в БД"));

            discoveredInterfaceRepository.clearConnectionInterfaceIdExcept(archInterfaceId, mapicInterfaceId);

        } else {
            discoveredInterface.setUpdatedDate(now);

            discoveredOperationRepository.clearConnectionOperationIdByDiscoveredInterfaceId(mapicInterfaceId);

        }
        discoveredOperationRepository.updateUpdatedDateByInterfaceId(mapicInterfaceId, now);
    }
}