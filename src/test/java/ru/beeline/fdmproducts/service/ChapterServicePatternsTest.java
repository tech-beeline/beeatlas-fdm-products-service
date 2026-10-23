package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.beeline.fdmproducts.client.TechradarClient;
import ru.beeline.fdmproducts.domain.Chapter;
import ru.beeline.fdmproducts.domain.ChapterPattern;
import ru.beeline.fdmproducts.dto.ProductAvailableDTO;
import ru.beeline.fdmproducts.dto.chapter.ChapterPatchRequestDTO;
import ru.beeline.fdmproducts.repository.ChapterPatternRepository;
import ru.beeline.fdmproducts.repository.ChapterRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChapterServicePatternsTest {

    private static final int CHAPTER_ID = 3;
    private static final int DELETED_PATTERN_ID = 10;

    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private ChapterPatternRepository chapterPatternRepository;
    @Mock
    private TechradarClient techradarClient;

    @InjectMocks
    private ChapterService chapterService;

    @BeforeEach
    void chapterWithLinkToDeletedPattern() {
        when(chapterRepository.findById(CHAPTER_ID))
                .thenReturn(Optional.of(Chapter.builder().id(CHAPTER_ID).code("CH-3").name("Оплата").build()));
        when(chapterPatternRepository.findAllByChapterId(CHAPTER_ID)).thenReturn(List.of(
                ChapterPattern.builder().id(100).chapterId(CHAPTER_ID).patternId(DELETED_PATTERN_ID).build()));
    }

    @Test
    void keepingAlreadyLinkedDeletedPattern_doesNotCallTechradar() {
        chapterService.patchChapter(CHAPTER_ID, null, patterns(DELETED_PATTERN_ID));

        verify(techradarClient, never()).checkPatternsAvailability(anyList());
        verify(chapterPatternRepository, never()).deleteAll(any());
        verify(chapterPatternRepository, never()).saveAll(any());
    }

    @Test
    void addingPattern_checksOnlyNewOne() {
        when(techradarClient.checkPatternsAvailability(List.of(20)))
                .thenReturn(ProductAvailableDTO.builder().availability(true).build());

        chapterService.patchChapter(CHAPTER_ID, null, patterns(DELETED_PATTERN_ID, 20));

        verify(techradarClient).checkPatternsAvailability(List.of(20));
        ArgumentCaptor<List<ChapterPattern>> saved = ArgumentCaptor.forClass(List.class);
        verify(chapterPatternRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ChapterPattern::getPatternId).containsExactly(20);
    }

    @Test
    void removingDeletedPattern_dropsLinkWithoutTechradar() {
        chapterService.patchChapter(CHAPTER_ID, null, patterns());

        verify(techradarClient, never()).checkPatternsAvailability(anyList());
        ArgumentCaptor<List<ChapterPattern>> removed = ArgumentCaptor.forClass(List.class);
        verify(chapterPatternRepository).deleteAll(removed.capture());
        assertThat(removed.getValue()).extracting(ChapterPattern::getPatternId).containsExactly(DELETED_PATTERN_ID);
    }

    @Test
    void addingUnavailablePattern_isRejected() {
        when(techradarClient.checkPatternsAvailability(List.of(30)))
                .thenReturn(ProductAvailableDTO.builder().availability(false).build());

        assertThatThrownBy(() -> chapterService.patchChapter(CHAPTER_ID, null, patterns(DELETED_PATTERN_ID, 30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("В массиве patterns переданы идентификаторы несуществующих паттернов");
        verify(chapterPatternRepository, never()).saveAll(any());
    }

    private ChapterPatchRequestDTO patterns(Integer... ids) {
        return ChapterPatchRequestDTO.builder().patterns(List.of(ids)).build();
    }
}
