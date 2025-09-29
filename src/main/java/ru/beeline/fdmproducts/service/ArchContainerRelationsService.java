package ru.beeline.fdmproducts.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.repository.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public void process(Integer entityId) {
        log.info("[СТАРТ] Начало обработки entityId={}", entityId);
        discoveredInterfaceRepository.clearConnectionInterfaceIdByEntityId(entityId);
        discoveredOperationRepository.clearConnectionOperationIdByEntityId(entityId);
    }
}
