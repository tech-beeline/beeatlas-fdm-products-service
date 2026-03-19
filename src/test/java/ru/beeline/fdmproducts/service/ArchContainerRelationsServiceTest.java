package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmproducts.domain.*;
import ru.beeline.fdmproducts.repository.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("local")          // ← включаем профиль «local»
@Transactional        // each test rolls back, leaving the DB clean
class ArchContainerRelationsServiceTest {

    @Autowired
    private ArchContainerRelationsService service;

    @Autowired
    private OperationRepository operationRepository;
    @Autowired
    private InterfaceRepository interfaceRepository;
    @Autowired
    private ContainerRepository containerRepository;
    @Autowired
    private DiscoveredInterfaceRepository discoveredInterfaceRepository;
    @Autowired
    private DiscoveredOperationRepository discoveredOperationRepository;

    @Test
    void processOperationComparison_matchesByNameAndType() {
        service.processOperationComparison(19099);

    }
}