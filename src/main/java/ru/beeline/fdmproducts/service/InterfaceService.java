/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.DiscoveredConnectionBranch;
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
    @Autowired
    private ProductBranchService productBranchService;


    public void handConnection(ConnectionRequestDTO request) {

        Integer mapicInterfaceId = request.getMapicInterfaceId();
        Integer archInterfaceId = request.getArchInterfaceId();

        if (mapicInterfaceId == null) {
            throw new IllegalArgumentException("Не передан обязательный параметр mapicInterfaceId");
        }

        DiscoveredInterface discoveredInterface = discoveredInterfaceRepository.findById(mapicInterfaceId)
                .orElseThrow(() -> new IllegalArgumentException("DiscoveredInterface отсутствует в БД"));

        DiscoveredConnectionBranch branch = null;
        if (archInterfaceId != null) {
            interfaceRepository.findById(archInterfaceId)
                    .orElseThrow(() -> new IllegalArgumentException("Interface отсутствует в БД"));
            branch = productBranchService.findConnectionBranchByInterfaceId(archInterfaceId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Сопоставление возможно только с интерфейсом веток main, develop или design"));
        }

        LocalDateTime now = LocalDateTime.now();
        discoveredInterface.setUpdatedDate(now);

        if (branch != null) {
            // Привязка пишется в атрибут ветки arch-интерфейса; связи двух других веток не меняются.
            branch.setConnectionInterfaceId(discoveredInterface, archInterfaceId);
            discoveredInterfaceRepository.clearConnectionInterfaceIdExcept(archInterfaceId, mapicInterfaceId,
                    branch.getBranchName());

        } else {
            // Снятие очищает все три ветки: иначе связи develop и design остаются, и автосопоставление
            // этот discovered-интерфейс больше не берёт.
            for (DiscoveredConnectionBranch connectionBranch : DiscoveredConnectionBranch.values()) {
                connectionBranch.setConnectionInterfaceId(discoveredInterface, null);
            }
            discoveredOperationRepository.clearConnectionOperationIdByDiscoveredInterfaceId(mapicInterfaceId);

        }
        discoveredOperationRepository.updateUpdatedDateByInterfaceId(mapicInterfaceId, now);
    }
}