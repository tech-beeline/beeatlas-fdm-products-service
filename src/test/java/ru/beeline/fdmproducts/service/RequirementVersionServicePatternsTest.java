package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.beeline.fdmproducts.client.BpmClient;
import ru.beeline.fdmproducts.client.FfManagerClient;
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnum;
import ru.beeline.fdmproducts.domain.NonFunctionalRequirementEnumCore;
import ru.beeline.fdmproducts.domain.PatternRequirement;
import ru.beeline.fdmproducts.dto.CreateRequirementRequestDTO;
import ru.beeline.fdmproducts.dto.ProductAvailableDTO;
import ru.beeline.fdmproducts.repository.ChapterNfrRepository;
import ru.beeline.fdmproducts.repository.ChapterRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumCoreRepository;
import ru.beeline.fdmproducts.repository.NonFunctionalRequirementEnumRepository;
import ru.beeline.fdmproducts.repository.PatternRequirementRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequirementVersionServicePatternsTest {

    private static final int CORE_ID = 7;
    private static final int OLD_VERSION_ID = 40;
    private static final int CURRENT_VERSION_ID = 41;
    private static final int DELETED_CURRENT_PATTERN = 10;
    private static final int OLD_VERSION_PATTERN = 30;

    @Mock
    private TechradarClient techradarClient;
    @Mock
    private FfManagerClient ffManagerClient;
    @Mock
    private BpmClient bpmClient;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private NonFunctionalRequirementEnumCoreRepository coreRepository;
    @Mock
    private NonFunctionalRequirementEnumRepository enumRepository;
    @Mock
    private ChapterNfrRepository chapterNfrRepository;
    @Mock
    private PatternRequirementRepository patternRequirementRepository;

    @InjectMocks
    private RequirementVersionService requirementVersionService;

    @BeforeEach
    void nfrWithTwoVersions() {
        TransactionSynchronizationManager.initSynchronization();
        NonFunctionalRequirementEnumCore core = NonFunctionalRequirementEnumCore.builder().id(CORE_ID).code("NFR-12").build();
        when(coreRepository.findById(CORE_ID)).thenReturn(Optional.of(core));
        when(enumRepository.findByCoreId(CORE_ID)).thenReturn(List.of(
                NonFunctionalRequirementEnum.builder().id(OLD_VERSION_ID).version(1).core(core).build(),
                NonFunctionalRequirementEnum.builder().id(CURRENT_VERSION_ID).version(2).core(core).build()));
        when(patternRequirementRepository.findByNfrId(CURRENT_VERSION_ID)).thenReturn(List.of(
                PatternRequirement.builder().patternId(DELETED_CURRENT_PATTERN).build()));
        when(patternRequirementRepository.findByNfrId(OLD_VERSION_ID)).thenReturn(List.of(
                PatternRequirement.builder().patternId(OLD_VERSION_PATTERN).build()));
        when(enumRepository.save(any())).thenAnswer(invocation -> {
            NonFunctionalRequirementEnum saved = invocation.getArgument(0);
            saved.setId(42);
            return saved;
        });
    }

    @AfterEach
    void clearSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void patternFromCurrentVersion_isNotRechecked() {
        requirementVersionService.createVersion(CORE_ID, null, request(DELETED_CURRENT_PATTERN));

        verify(techradarClient, never()).checkPatternsAvailability(anyList());
        verify(patternRequirementRepository).saveAll(any());
    }

    @Test
    void newPattern_isCheckedAlone() {
        when(techradarClient.checkPatternsAvailability(List.of(20)))
                .thenReturn(ProductAvailableDTO.builder().availability(true).build());

        requirementVersionService.createVersion(CORE_ID, null, request(DELETED_CURRENT_PATTERN, 20));

        verify(techradarClient).checkPatternsAvailability(List.of(20));
    }

    @Test
    void patternOnlyFromOldVersion_isCheckedAsNew() {
        when(techradarClient.checkPatternsAvailability(List.of(OLD_VERSION_PATTERN)))
                .thenReturn(ProductAvailableDTO.builder().availability(false).build());

        assertThatThrownBy(() -> requirementVersionService.createVersion(CORE_ID, null, request(OLD_VERSION_PATTERN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("В массиве patterns переданы идентификаторы несуществующих паттернов");
        verify(enumRepository, never()).save(any());
    }

    private CreateRequirementRequestDTO request(Integer... patterns) {
        return CreateRequirementRequestDTO.builder()
                .name("Время отклика")
                .description("Описание")
                .patterns(List.of(patterns))
                .build();
    }
}
