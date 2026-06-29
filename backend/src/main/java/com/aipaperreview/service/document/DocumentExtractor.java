package com.aipaperreview.service.document;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentExtractor {
    boolean supports(String filename);

    ExtractedDocument extract(String filename, InputStream inputStream) throws IOException;
}
