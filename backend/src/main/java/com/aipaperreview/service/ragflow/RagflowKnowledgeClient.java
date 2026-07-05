package com.aipaperreview.service.ragflow;

import java.util.List;

public interface RagflowKnowledgeClient {
    boolean isEnabled();

    List<RagflowRetrievedChunk> retrieve(String question);
}
