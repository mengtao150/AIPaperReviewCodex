INSERT IGNORE INTO review_type (id, name, description, classification_rules, active) VALUES
(1, 'Overview of Reviews', '系统检索并纳入系统评价类研究的综述', '文献检索系统，并纳入了系统评价类研究。', true),
(2, 'Scoping Review', '系统全面检索并对纳入研究进行质量评估的综述', '文献检索系统、全面，并对纳入研究进行了质量评估。', true),
(3, 'Systematic Review', '系统全面检索但未报告质量评估的综述', '文献检索系统、全面，但未对纳入研究进行质量评估。', true),
(4, 'Rapid Review', '检索存在时间限制的快速综述', '文献检索有明确时间限制。', true),
(5, 'Mini Review', '篇幅较短、聚焦关键问题的综述', '文章字数较少、篇幅较短。', true),
(6, 'Clinical Practice Review', '基于临床经验、专家观点或实践意见的综述', '文章主要基于专家观点、临床意见或经验进行文献讨论。', true),
(7, 'Narrative Review', '不符合前述条件时归为叙述性综述', '不属于 Overview、Scoping、Systematic、Rapid、Mini 或 Clinical Practice Review 时，判断为 Narrative Review。', true);

INSERT IGNORE INTO structure_template (id, review_type_id, name, version, source_file_path, parsed_sections, required_items, optional_items, active, created_at, updated_at) VALUES
(1, 7, '通用 Review Article 文章结构', '1.0', 'docx/文章结构.docx',
'Title Page; Abstract; Keywords; Introduction; Main body; Conclusions; Acknowledgments; Footnote; References; Tables; Figures; Legends; Supplementary Appendix',
'Title Page: title, authors, affiliation, correspondence, running title, word count, contributions.
Abstract: unstructured, 200-350 words, accurately describes article content.
Keywords: 3-5 keywords.
Introduction: Background; Rationale and knowledge gap; Objective; checklist statement if applicable.
Main body: authors may define subheadings; must discuss strengths and limitations of the review.
Conclusions: data-supported conclusions.
Footnote: reporting checklist, funding, conflicts of interest, ethical statement.
References: appropriate number, up to date, proper citations.
Tables/Figures/Videos/Legends: clear, high resolution, annotated, consistent with text.',
'Acknowledgments; Tables; Figures; Videos; Supplementary Appendix', true, NOW(), NOW()),
(2, 5, 'Mini Review 文章结构', '1.0', 'docx/文章结构.docx',
'Title Page; Abstract; Keywords; Introduction; Main body; Conclusions; References',
'Title Page; Abstract; Keywords; Introduction with background, rationale and objective; focused main body; conclusions; references.', 'Tables; Figures; Supplementary Appendix', true, NOW(), NOW()),
(3, 6, 'Clinical Practice Review 文章结构', '1.0', 'docx/文章结构.docx',
'Title Page; Abstract; Keywords; Introduction; Main body; Conclusions; References',
'Title Page; Abstract; Keywords; Introduction with background, rationale and objective; main body with literature findings and clinical experience; conclusions.', 'Tables; Figures; Supplementary Appendix', true, NOW(), NOW());

INSERT IGNORE INTO checklist_template (id, review_type_id, name, version, source_file_path, notes, active, created_at, updated_at) VALUES
(1, 7, 'AME Narrative Review 审核清单', '2025.6.11', 'docx/综述清单.pdf', '从现有综述清单 PDF 抽取的 Narrative Review 示例清单。', true, NOW(), NOW()),
(2, 5, 'AME Mini Review 审核清单', '2025.6.11', 'docx/综述清单.pdf', '从现有综述清单 PDF 抽取的 Mini Review 示例清单。', true, NOW(), NOW()),
(3, 6, 'AME Clinical Practice Review 审核清单', '2025.6.11', 'docx/综述清单.pdf', '从现有综述清单 PDF 抽取的 Clinical Practice Review 示例清单。', true, NOW(), NOW());

INSERT IGNORE INTO checklist_item (id, checklist_template_id, category, requirement, required_item, evaluation_guidance, order_index) VALUES
(1, 1, '题目', '重点突出，涵盖全面；定位明确为 Narrative Review；更新类综述需在标题中明确标注 updated，建议表明检索时间范围。', true, '检查标题是否体现文章主题和 Narrative Review 类型。', 1),
(2, 1, '摘要', 'Background and Objective：简述研究背景，陈述做此综述的缘由，明确综述目标。', true, '检查摘要是否包含背景、缘由和目标。', 2),
(3, 1, '摘要', 'Methods：简述搜索策略，如数据库、时间框架和语言考虑因素。', true, '检查是否说明检索数据库、时间范围、语言等。', 3),
(4, 1, '摘要', 'Key Content and Findings：描述文献综述的主要内容和主要发现。', true, '检查摘要是否概括主要内容和发现。', 4),
(5, 1, '摘要', 'Conclusions：描述主要结论以及对未来研究、临床实践和政策制定的潜在影响。', true, '检查摘要结论是否有意义和影响说明。', 5),
(6, 1, '关键词', '关键词数量为 3-5 个，且与主题密切相关。', true, '检查关键词数量和相关性。', 6),
(7, 1, '引言', 'Background：详细描述研究背景，如疾病或药物背景、流行病学、当前治疗局限性等。', true, '检查引言背景是否充分。', 7),
(8, 1, '引言', 'Rationale and knowledge gap：说明撰写综述的原因，以及现存文献不能解决的问题。', true, '检查是否明确 knowledge gap 和创新点。', 8),
(9, 1, '引言', 'Objective：写明具体目标，例如 aim to 或 objective is。', true, '检查是否有明确研究目的。', 9),
(10, 1, '方法', '报告检索日期，且具体到年月日。', true, '不能只写近期、近年来等模糊表述。', 10),
(11, 1, '方法', '报告检索时间范围，且具体到年月日。', true, '建议形如 2010.1-2022.4。', 11),
(12, 1, '方法', '报告数据库、检索词、检索策略、纳排标准和文献筛选过程。', true, '检索策略应覆盖综述讨论范围，并说明筛选过程。', 12),
(13, 1, '主要内容', '概述文献结果，评价纳入文献局限性和/或质量，总结综述优势与局限，提出未来研究想法，并总结图表。', true, '检查主体是否不只是罗列文献，而有分析和评价。', 13),
(14, 1, '结论', '说明本综述对临床实践、政策或未来研究的意义。', true, '结论需要被正文数据或论述支持。', 14),
(15, 2, '题目', '重点突出，涵盖全面；定位明确为 Mini Review。', true, '检查标题是否聚焦且能体现综述类型。', 1),
(16, 2, '摘要', '简述研究背景、综述缘由、综述目标、主要发现、临床意义或未来研究方向。', true, '检查摘要是否完整覆盖这些信息。', 2),
(17, 2, '关键词', '关键词数量为 3-5 个，且与主题密切相关。', true, '检查关键词数量和相关性。', 3),
(18, 2, '引言', '详细描述背景、撰写原因、现存文献不能解决的问题和具体目标。', true, '检查引言是否包含 background、rationale/knowledge gap、objective。', 4),
(19, 2, '主要内容', '针对关键问题概括已有文献结果，评价纳入文献局限性和/或质量，总结本综述优势与局限，提出未来研究想法。', true, '图表总结非强制，但应视情况判断是否需要。', 5),
(20, 2, '结论', '说明本综述对临床实践、政策或未来研究的意义。', true, '检查结论是否具体、受正文支持。', 6),
(21, 3, '题目', '重点突出，涵盖全面；定位明确，标题中体现 Clinical Practice Review，例如 clinical 或 experience。', true, '检查标题是否体现 CPR 定位。', 1),
(22, 3, '摘要', '简述研究背景、综述缘由、综述目标、主要发现、临床意义或未来研究方向。', true, '检查摘要是否体现临床实践意义。', 2),
(23, 3, '关键词', '关键词数量为 3-5 个，且与主题密切相关。', true, '检查关键词数量和相关性。', 3),
(24, 3, '引言', '结构化为 Background、Rationale and knowledge gap、Objective，并详细描述背景、原因、知识空白和具体目标。', true, '检查引言结构和内容是否完整。', 4),
(25, 3, '主要内容', '概括已有文献的重要发现，评价纳入文献局限性和/或质量，总结自身临床经验，为临床实践提供指导建议。', true, 'Clinical Practice Review 需要体现临床经验和实践建议。', 5),
(26, 3, '结论', '说明本综述对临床实践、政策或未来研究的意义。', true, '检查结论是否有明确实践或研究意义。', 6);
