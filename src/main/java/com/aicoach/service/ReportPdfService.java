package com.aicoach.service;

import com.aicoach.dto.InterviewDtos.QuestionFeedback;
import com.aicoach.dto.InterviewDtos.ReportResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ReportPdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 20, Font.BOLD, Color.decode("#1e293b"));
    private static final Font H2_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, Color.decode("#334155"));
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.decode("#0f172a"));
    private static final Font MUTED_FONT = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.decode("#64748b"));

    public byte[] generate(ReportResponse report) {
        try {
            Document document = new Document(PageSize.A4, 48, 48, 48, 48);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("StudyBuddy AI - Interview Report", TITLE_FONT));
            document.add(Chunk.NEWLINE);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
            document.add(new Paragraph("Type: " + report.type() + "   |   Role: " +
                    (report.targetRole() != null ? report.targetRole() : "N/A"), BODY_FONT));
            if (report.completedAt() != null) {
                document.add(new Paragraph("Completed: " + report.completedAt().format(fmt), MUTED_FONT));
            }
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Scores", H2_FONT));
            document.add(new Paragraph("Overall Score: " + report.overallScore() + " / 100", BODY_FONT));
            document.add(new Paragraph("Communication: " + report.communicationScore() + " / 100", BODY_FONT));
            document.add(new Paragraph("Confidence: " + report.confidenceScore() + " / 100", BODY_FONT));
            document.add(new Paragraph("Knowledge: " + report.knowledgeScore() + " / 100", BODY_FONT));
            document.add(Chunk.NEWLINE);

            if (report.summary() != null) {
                document.add(new Paragraph("Summary", H2_FONT));
                document.add(new Paragraph(report.summary(), BODY_FONT));
                document.add(Chunk.NEWLINE);
            }

            addBulletSection(document, "Strengths", report.strengths());
            addBulletSection(document, "Weaknesses", report.weaknesses());
            addBulletSection(document, "Improvement Plan", report.improvementPlan());

            document.add(new Paragraph("Question-wise Feedback", H2_FONT));
            document.add(Chunk.NEWLINE);
            int i = 1;
            for (QuestionFeedback qf : report.questionFeedback()) {
                document.add(new Paragraph("Q" + (i++) + ". " + qf.questionText(), BODY_FONT));
                Paragraph answer = new Paragraph("Answer: " + qf.answerText(), MUTED_FONT);
                answer.setIndentationLeft(12);
                document.add(answer);
                Paragraph score = new Paragraph("Score: " + qf.score() + "/100 — " + qf.feedback(), BODY_FONT);
                score.setIndentationLeft(12);
                document.add(score);
                document.add(Chunk.NEWLINE);
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void addBulletSection(Document document, String title, java.util.List<String> items) throws DocumentException {
        if (items == null || items.isEmpty()) return;
        document.add(new Paragraph(title, H2_FONT));
        for (String item : items) {
            document.add(new Paragraph("\u2022 " + item, BODY_FONT));
        }
        document.add(Chunk.NEWLINE);
    }
}
