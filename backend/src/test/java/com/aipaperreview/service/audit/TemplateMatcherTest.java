package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aipaperreview.domain.StructureTemplate;
import com.aipaperreview.repository.StructureTemplateRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TemplateMatcherTest {
    @Test
    void matchesActiveTemplateByReviewTypeName() {
        StructureTemplateRepository repository = mock(StructureTemplateRepository.class);
        StructureTemplate template = new StructureTemplate();
        template.setName("Narrative template");
        when(repository.findFirstByReviewType_NameIgnoreCaseAndActiveTrueOrderByVersionDesc("Narrative Review"))
                .thenReturn(Optional.of(template));

        TemplateMatcher matcher = new TemplateMatcher(repository);

        assertThat(matcher.match("Narrative Review")).contains(template);
    }
}
