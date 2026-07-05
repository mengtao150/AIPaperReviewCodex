package com.aipaperreview.web;

import com.aipaperreview.domain.AuditJob;
import com.aipaperreview.repository.AuditJobRepository;
import com.aipaperreview.service.audit.AuditOrchestrator;
import com.aipaperreview.service.audit.ManuscriptParagraphService;
import com.aipaperreview.service.audit.WordReportBuilder;
import com.aipaperreview.web.dto.AuditJobResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin
@RestController
@RequestMapping("/api/audits")
public class AuditController {
    private final AuditOrchestrator auditOrchestrator;
    private final AuditJobRepository auditJobRepository;
    private final ManuscriptParagraphService manuscriptParagraphService;
    private final WordReportBuilder wordReportBuilder;

    public AuditController(
            AuditOrchestrator auditOrchestrator,
            AuditJobRepository auditJobRepository,
            ManuscriptParagraphService manuscriptParagraphService,
            WordReportBuilder wordReportBuilder
    ) {
        this.auditOrchestrator = auditOrchestrator;
        this.auditJobRepository = auditJobRepository;
        this.manuscriptParagraphService = manuscriptParagraphService;
        this.wordReportBuilder = wordReportBuilder;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AuditJobResponse create(@RequestPart("file") MultipartFile file) throws IOException {
        return AuditJobResponse.from(auditOrchestrator.startAudit(file));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AuditJobResponse> list() {
        return auditJobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AuditJobResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional
    public AuditJobResponse get(@PathVariable Long id) {
        AuditJob job = auditJobRepository.findById(id).orElseThrow();
        return AuditJobResponse.from(manuscriptParagraphService.ensureParagraphs(job));
    }

    @GetMapping("/{id}/report.docx")
    public ResponseEntity<byte[]> downloadWordReport(@PathVariable Long id) throws IOException {
        AuditJob job = auditJobRepository.findById(id).orElseThrow();
        byte[] bytes = wordReportBuilder.build(job);
        String filename = sanitizeFilename(job.getOriginalFilename()) + "-审查报告.docx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(URLEncoder.encode(filename, StandardCharsets.UTF_8), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(bytes);
    }

    private String sanitizeFilename(String filename) {
        String value = filename == null || filename.isBlank() ? "稿件" : filename;
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
