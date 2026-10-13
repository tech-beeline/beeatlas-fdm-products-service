package ru.beeline.fdmproducts.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.beeline.fdmproducts.domain.Chapter;
import ru.beeline.fdmproducts.domain.ChapterPattern;
import ru.beeline.fdmproducts.dto.PatternUsageDTO;
import ru.beeline.fdmproducts.dto.nfr.NfrItemDTO;
import ru.beeline.fdmproducts.repository.ChapterPatternRepository;
import ru.beeline.fdmproducts.repository.ChapterRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatternUsageServiceTest {

    @Mock
    private ChapterPatternRepository chapterPatternRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private PatternRequirementService patternRequirementService;

    @InjectMocks
    private PatternUsageService patternUsageService;

    @Test
    void usedPattern_returnsChaptersSortedByCodeAndActualNfr() {
        when(chapterPatternRepository.findAllByPatternId(7)).thenReturn(List.of(
                ChapterPattern.builder().chapterId(5).patternId(7).build(),
                ChapterPattern.builder().chapterId(3).patternId(7).build()));
        when(chapterRepository.findAllById(any())).thenReturn(List.of(
                Chapter.builder().id(5).code("CH-5").name("Доставка").build(),
                Chapter.builder().id(3).code("CH-3").name("Оплата").build()));
        when(patternRequirementService.getNfrByPatternId(7)).thenReturn(List.of(
                NfrItemDTO.builder().id(41).code("NFR-12").version(2).name("Время отклика").description("d").build()));

        PatternUsageDTO usage = patternUsageService.getUsage("7");

        assertThat(usage.getChapters()).extracting(PatternUsageDTO.ChapterItem::getCode)
                .containsExactly("CH-3", "CH-5");
        assertThat(usage.getChapters().get(0).getId()).isEqualTo(3);
        assertThat(usage.getChapters().get(0).getName()).isEqualTo("Оплата");
        assertThat(usage.getNfr()).singleElement().satisfies(nfr -> {
            assertThat(nfr.getId()).isEqualTo(41);
            assertThat(nfr.getCode()).isEqualTo("NFR-12");
            assertThat(nfr.getVersion()).isEqualTo(2);
            assertThat(nfr.getName()).isEqualTo("Время отклика");
        });
    }

    @Test
    void unusedPattern_returnsEmptyLists() {
        when(chapterPatternRepository.findAllByPatternId(9)).thenReturn(List.of());
        when(patternRequirementService.getNfrByPatternId(9)).thenReturn(List.of());

        PatternUsageDTO usage = patternUsageService.getUsage("9");

        assertThat(usage.getChapters()).isEmpty();
        assertThat(usage.getNfr()).isEmpty();
        verify(chapterRepository, never()).findAllById(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "0", "-1", "1.5", " "})
    void invalidId_isRejected(String id) {
        assertThatThrownBy(() -> patternUsageService.getUsage(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректный id");
    }
}
