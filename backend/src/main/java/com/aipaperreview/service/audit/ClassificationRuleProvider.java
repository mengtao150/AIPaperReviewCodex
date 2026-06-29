package com.aipaperreview.service.audit;

import org.springframework.stereotype.Component;

@Component
public class ClassificationRuleProvider {
    public String reviewTypeFlowchartRules() {
        return """
                请严格按照以下 Review 类型判别图逻辑判断稿件类型，不能只看题目，必须阅读全文：
                1. 如果文献检索是系统性的，并且纳入了系统评价类研究，则判断为 Overview of Reviews。
                2. 否则，如果文献检索系统且全面，并且对纳入研究进行了质量评估，例如偏倚风险评估，则判断为 Scoping Review。
                3. 否则，如果文献检索系统且全面，但没有对纳入研究进行质量评估，则判断为 Systematic Review。
                4. 否则，如果文献检索有明确时间限制，则判断为 Rapid Review。
                5. 否则，如果文章字数较少、篇幅短，则判断为 Mini Review。
                6. 否则，如果文章主要基于专家观点、临床意见或经验进行文献讨论，则判断为 Clinical Practice Review。
                7. 否则判断为 Narrative Review。

                输出必须是 JSON，字段包括：
                review_type, confidence, decision_path, evidence, uncertainties, needs_human_review。
                """;
    }
}
