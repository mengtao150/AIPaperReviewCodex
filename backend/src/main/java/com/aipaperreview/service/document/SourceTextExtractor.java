package com.aipaperreview.service.document;

import java.io.IOException;
import java.io.InputStream;

public interface SourceTextExtractor {
    String extract(String filename, InputStream inputStream) throws IOException;
}
