package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aipaperreview.domain.AuditJob;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class WordReportBuilderTest {
    @Test
    void buildsWordDocumentFromAuditJobMarkdownReport() throws IOException {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("manuscript.pdf");
        job.setFinalReport("""
                # 综述稿件审查报告

                ## 稿件

                manuscript.pdf

                ## 修改建议

                建议补充检索策略。
                """);

        byte[] bytes = new WordReportBuilder().build(job);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (left, right) -> left + "\n" + right);

            assertThat(text).contains("综述稿件审查报告");
            assertThat(text).contains("manuscript.pdf");
            assertThat(text).contains("建议补充检索策略");
        }
    }

    @Test
    void buildsReviewerReadableReportFromAuditFieldsInsteadOfRawJson() throws IOException {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("manuscript.pdf");
        job.setClassificationResult("""
                {"review_type":"Narrative Review","reason":"稿件围绕 LCBDE 与 ERCP+LC 的比较展开。"}
                """);
        job.setStructureAuditResult("""
                [
                  {
                    "item": "Title Page",
                    "status": "撰写不完整",
                    "evidence": "标题页缺少作者和单位。",
                    "reason": "标题页信息不完整，审稿人无法确认作者和机构来源。",
                    "suggestion": "补充作者、单位和通信作者信息。"
                  }
                ]
                """);
        job.setChecklistAuditResult("""
                <think>
                For item 13 (主要内容):
                - 内容科学性: 核心结论有数据支撑。
                - 逻辑性: 主体按疗效、安全性、住院时间和成本展开。
                - 证据支撑: 部分定量结论需要补充具体引用编号。
                - 综述分析深度: 已讨论学习曲线和机构资源差异。
                - 图表与正文呼应: Table X 未在主体部分充分总结。
                </think>
                {"item":"[主要内容] 主体内容","status":"撰写不完整","evidence":"Main Body 涵盖 Ductal Clearance、Complications、Cost 和 Patient Satisfaction，但未总结 Table X。","reason":"【内容科学性】核心结论有数据支撑。【逻辑性】主体小节递进清楚。【证据支撑】部分定量结论需补充引用编号。【综述分析深度】已讨论技术和机构因素。【图表与正文呼应】Table X 未被主体充分解释。","suggestion":"在主体部分增加图表总结，并补充未来研究方向。","suggested_location":"Main Body"}
                """);
        job.setFinalReport("""
                # 综述审查报告

                {"item":"Title Page","status":"撰写不完整"}
                """);

        byte[] bytes = new WordReportBuilder().build(job);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = documentText(document);

            assertThat(text).contains("稿件基本信息");
            assertThat(text).contains("文章结构审查结果");
            assertThat(text).contains("审核清单审查结果");
            assertThat(text).contains("Title Page");
            assertThat(text).contains("标题页缺少作者和单位");
            assertThat(text).contains("[主要内容] 主体内容");
            assertThat(text).contains("内容科学性");
            assertThat(text).contains("图表与正文呼应");
            assertThat(text).doesNotContain("{\"item\"");
            assertThat(text).doesNotContain("\"status\"");
            assertThat(text).doesNotContain("```json");
        }
    }

    @Test
    void includesRagflowEnhancedAuditInReviewerReadableWordReport() throws IOException {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("manuscript.pdf");
        job.setClassificationResult("{\"review_type\":\"Narrative Review\"}");
        job.setRagflowEnhancedAuditResult("""
                [
                  {
                    "item": "Major问题：正文缺少临床决策流程图",
                    "status": "Major问题",
                    "evidence": "正文比较 LCBDE 与 ERCP，但未说明不同患者场景下的适用路径。",
                    "reason": "历史人工审稿意见强调此类临床综述需要给出适应证和决策流程。",
                    "suggestion": "增加一张决策流程图，并在正文解释适用条件。"
                  }
                ]
                """);

        byte[] bytes = new WordReportBuilder().build(job);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = documentText(document);

            assertThat(text).contains("人工经验增强审查结果");
            assertThat(text).contains("正文缺少临床决策流程图");
            assertThat(text).contains("历史人工审稿意见强调");
            assertThat(text).doesNotContain("{\"item\"");
        }
    }

    @Test
    void reviewerReportFocusesOnProblemsAndRemovesMachineFormattedContent() throws IOException {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("manuscript.pdf");
        job.setStatus(AuditJob.Status.COMPLETED);
        job.setClassificationResult("""
                {"review_type":"Narrative Review","evidence":"这是一段很长的类型判定证据，不应在 Word 审稿意见书中完整展开。"}
                """);
        job.setStructureAuditResult("""
                [
                  {
                    "item": "Title Page > title",
                    "status": "已撰写",
                    "evidence": "标题已经存在。",
                    "reason": "这个已完成项目不应进入审稿人 Word 报告。",
                    "suggestion": "无需修改。"
                  },
                  {
                    "item": "Title Page > authors",
                    "status": "未撰写",
                    "evidence": "题首页未列出作者姓名、单位和通信作者信息。",
                    "reason": "缺少作者信息会影响审稿人与编辑判断稿件来源。",
                    "suggestion": "补充完整作者、单位、通信作者和作者贡献信息。",
                    "suggested_location": "Title Page"
                  }
                ]
                """);
        job.setChecklistAuditResult("""
                <think>
                模型内部分析：这里包含大量全文复述和推理过程，Word 中不应显示。
                </think>
                {"item":"[主要内容] 主体内容","status":"撰写不完整","evidence":"正文仅罗列 Ductal Clearance、Complications、Cost 等内容，但缺少研究设计、样本量、主要结局总结表。这里故意放入一段很长很长的证据文本，用来验证 Word 报告会进行短摘处理，而不是把模型输出的全文或长段落原样塞进报告，导致审稿人阅读非常难受。","reason":"主体内容有比较，但缺少对证据质量、适用条件和研究差异的综合评价。","suggestion":"增加研究特征总结表，并在正文说明不同研究设计和样本量对结论可信度的影响。","suggested_location":"Main Body"}
                """);
        job.setRagflowEnhancedAuditResult("""
                [
                  {
                    "item": "Major问题：缺少临床决策流程图",
                    "status": "Major问题",
                    "evidence": "正文比较 LCBDE 与 ERCP，但没有按患者条件、机构资源和技术能力说明适用路径。",
                    "reason": "历史人工审稿意见强调此类临床综述需要给出适应证和决策流程。",
                    "suggestion": "增加决策流程图，并在正文解释不同场景下的选择标准。"
                  }
                ]
                """);
        job.setFinalReport("""
                ```json
                {"raw":"这段 JSON 不应出现在 Word 报告中"}
                ```
                """);

        byte[] bytes = new WordReportBuilder().build(job);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = documentText(document);
            String fontNames = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .map(run -> run.getFontFamily() == null ? "" : run.getFontFamily())
                    .reduce("", (left, right) -> left + "\n" + right);

            assertThat(text).contains("综述稿件审查意见书");
            assertThat(text).contains("重点问题概览");
            assertThat(text).contains("Title Page > authors");
            assertThat(text).contains("主体内容");
            assertThat(text).contains("缺少临床决策流程图");
            assertThat(text).contains("问题位置");
            assertThat(text).contains("修改建议");
            assertThat(text).doesNotContain("Title Page > title");
            assertThat(text).doesNotContain("这个已完成项目不应进入审稿人 Word 报告");
            assertThat(text).doesNotContain("模型内部分析");
            assertThat(text).doesNotContain("```json");
            assertThat(text).doesNotContain("{\"raw\"");
            assertThat(text).doesNotContain("COMPLETED");
            assertThat(text).doesNotContain("这里故意放入一段很长很长的证据文本，用来验证 Word 报告会进行短摘处理，而不是把模型输出的全文或长段落原样塞进报告");
            assertThat(fontNames).doesNotContain("Consolas");
        }
    }

    @Test
    void reviewerReportIgnoresEmptyAuditObjects() throws IOException {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("manuscript.pdf");
        job.setStatus(AuditJob.Status.COMPLETED);
        job.setClassificationResult("{\"review_type\":\"Narrative Review\"}");
        job.setStructureAuditResult("{}");
        job.setChecklistAuditResult("""
                [
                  {
                    "item": "Abstract - Methods",
                    "status": "撰写不完整",
                    "reason": "摘要方法部分未说明检索语言限制。",
                    "suggestion": "补充语言限制说明。"
                  }
                ]
                """);

        byte[] bytes = new WordReportBuilder().build(job);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = documentText(document);

            assertThat(text).contains("Abstract - Methods");
            assertThat(text).doesNotContain("文章结构审查（无法判断）");
            assertThat(text).doesNotContain("问题级别：无法判断");
        }
    }

    @Test
    void reviewerReportHidesInvalidRawAuditText() throws IOException {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("manuscript.pdf");
        job.setStatus(AuditJob.Status.COMPLETED);
        job.setClassificationResult("{\"review_type\":\"Narrative Review\"}");
        job.setStructureAuditResult("<think>raw reasoning</think> model returned invalid text");
        job.setChecklistAuditResult("""
                [
                  {
                    "item": "Abstract - Methods",
                    "status": "撰写不完整",
                    "reason": "摘要方法部分未说明检索语言限制。",
                    "suggestion": "补充语言限制说明。"
                  }
                ]
                """);

        byte[] bytes = new WordReportBuilder().build(job);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = documentText(document);

            assertThat(text).contains("Abstract - Methods");
            assertThat(text).doesNotContain("raw reasoning");
            assertThat(text).doesNotContain("invalid text");
            assertThat(text).doesNotContain("输出格式异常");
            assertThat(text).doesNotContain("问题级别：无法判断");
        }
    }

    private String documentText(XWPFDocument document) {
        return document.getParagraphs().stream()
                .map(paragraph -> paragraph.getText())
                .reduce("", (left, right) -> left + "\n" + right);
    }
}
