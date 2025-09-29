package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.repository.*;

@Transactional
@Service
@Slf4j
public class ArchContainerRelationsService {
    private final DiscoveredOperationRepository discoveredOperationRepository;
    private final DiscoveredInterfaceRepository discoveredInterfaceRepository;

    public ArchContainerRelationsService(DiscoveredOperationRepository discoveredOperationRepository,
                                         DiscoveredInterfaceRepository discoveredInterfaceRepository) {
        this.discoveredOperationRepository = discoveredOperationRepository;
        this.discoveredInterfaceRepository = discoveredInterfaceRepository;
    }

    public void processContainerDelete(Integer entityId) {
        log.info("[СТАРТ] Начало обработки processContainerDelete entityId={}", entityId);
        discoveredInterfaceRepository.clearConnectionInterfaceIdByEntityId(entityId);
        discoveredOperationRepository.clearConnectionOperationIdByEntityId(entityId);
    }

    public void processInterafaceDelete(int entityId) {
        log.info("[СТАРТ] Начало обработки processInterafaceDelete entityId={}", entityId);
        discoveredInterfaceRepository.clearConnectionInterfaceIdByInterfaceId(entityId);
        discoveredOperationRepository.clearConnectionOperationIdByInterfaceId(entityId);
    }
}
