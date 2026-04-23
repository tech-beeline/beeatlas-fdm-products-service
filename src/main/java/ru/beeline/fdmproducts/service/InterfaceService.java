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
import ru.beeline.fdmproducts.repository.InterfaceRepository;

@Transactional
@Service
@Slf4j
public class InterfaceService {
    @Autowired
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Autowired
    private InterfaceRepository interfaceRepository;


    public void handConnection(ConnectionRequestDTO request) {

        DiscoveredInterface discoveredInterface = discoveredInterfaceRepository.findById(request.getMapicInterfaceId())
                .orElseThrow(() -> new IllegalArgumentException("discoveredInterface отсутствует в БЖ"));
        interfaceRepository.findById(request.getArchInterfaceId())
                .orElseThrow(() -> new IllegalArgumentException("Interface отсутствует в БЖ"));
        discoveredInterface.setConnectionInterfaceId(request.getArchInterfaceId());
        discoveredInterfaceRepository.clearConnectionInterfaceIdExcept(request.getArchInterfaceId(),
                                                                       request.getMapicInterfaceId());

    }
}