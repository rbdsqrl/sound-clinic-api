package com.simplehearing.discharge.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.simplehearing.discharge.dto.DischargeRecordResponse;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Renders a discharge episode's report as a PDF — programmatic layout via OpenPDF, no templating engine. */
@Service
public class DischargePdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private static final Font TITLE_FONT  = new Font(Font.HELVETICA, 20, Font.BOLD);
    private static final Font HEADING_FONT = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(0x37, 0x41, 0x51));
    private static final Font BODY_FONT   = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font MUTED_FONT  = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Color ACCENT = new Color(0x63, 0x66, 0xf1);

    public byte[] generate(DischargeRecordResponse r, String patientName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 48, 48, 56, 56);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Discharge Summary", TITLE_FONT));
            doc.add(new Paragraph(patientName, HEADING_FONT));
            doc.add(spacer(4));
            doc.add(new Paragraph("Discharged " + fmt(r.dischargeDate())
                    + (r.episodeStartDate() != null ? "  ·  episode started " + fmt(r.episodeStartDate()) : ""), BODY_FONT));
            doc.add(spacer(16));

            doc.add(new Paragraph("Programs in this episode", HEADING_FONT));
            doc.add(spacer(6));
            if (r.enrollments().isEmpty()) {
                doc.add(new Paragraph("No programs on record.", BODY_FONT));
            } else {
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2.2f, 1.8f, 1f, 1f});
                addHeaderCell(table, "Program");
                addHeaderCell(table, "Therapist");
                addHeaderCell(table, "Start");
                addHeaderCell(table, "End");
                for (DischargeRecordResponse.EnrollmentSummary e : r.enrollments()) {
                    table.addCell(cell(e.programName()));
                    table.addCell(cell(e.therapistName()));
                    table.addCell(cell(e.startDate() != null ? fmt(e.startDate()) : "—"));
                    table.addCell(cell(e.endDate() != null ? fmt(e.endDate()) : "—"));
                }
                doc.add(table);
            }
            doc.add(spacer(16));

            doc.add(new Paragraph("Success criteria", HEADING_FONT));
            doc.add(spacer(6));
            PdfPTable criteria = new PdfPTable(2);
            criteria.setWidthPercentage(100);
            criteria.setWidths(new float[]{1.4f, 1f});
            addCriteriaRow(criteria, "Goal mastery",
                    r.goalMasteryPct() != null ? r.goalMasteryPct() + "%" : "No data",
                    Boolean.TRUE.equals(r.goalMasteryMet()));
            addCriteriaRow(criteria, "Parent satisfaction",
                    r.avgProgressRatingPct() != null ? r.avgProgressRatingPct() + "%" : "No data",
                    Boolean.TRUE.equals(r.parentSatisfactionMet()));
            addCriteriaRow(criteria, "Therapist sign-off",
                    r.therapistSignoffMet() ? "Confirmed" : "Not confirmed",
                    r.therapistSignoffMet());
            doc.add(criteria);
            doc.add(spacer(10));

            Font overallFont = new Font(Font.HELVETICA, 12, Font.BOLD, r.overallSuccessful() ? new Color(0x16, 0x8a, 0x5c) : new Color(0xb4, 0x53, 0x09));
            doc.add(new Paragraph(r.overallSuccessful() ? "Overall: Successful Completion" : "Overall: Not all criteria were met", overallFont));
            doc.add(spacer(16));

            if (r.avgCommunicationRating() != null) {
                doc.add(new Paragraph("Average communication rating: " + r.avgCommunicationRating() + " / 5", BODY_FONT));
                doc.add(spacer(10));
            }

            if (r.notes() != null && !r.notes().isBlank()) {
                doc.add(new Paragraph("Notes", HEADING_FONT));
                doc.add(spacer(6));
                doc.add(new Paragraph(r.notes(), BODY_FONT));
                doc.add(spacer(16));
            }

            doc.add(spacer(20));
            doc.add(new Paragraph("Discharged by " + r.dischargedByName() + " on " + fmt(r.dischargeDate()), MUTED_FONT));
            doc.add(new Paragraph("Generated " + fmt(LocalDate.now()), MUTED_FONT));

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate discharge PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(ACCENT);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private static com.lowagie.text.pdf.PdfPCell cell(String text) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new Phrase(text, BODY_FONT));
        cell.setPadding(6);
        return cell;
    }

    private static void addCriteriaRow(PdfPTable table, String label, String value, boolean met) {
        table.addCell(cell(label));
        Font valueFont = new Font(Font.HELVETICA, 10, Font.BOLD, met ? new Color(0x16, 0x8a, 0x5c) : Color.DARK_GRAY);
        com.lowagie.text.pdf.PdfPCell valueCell = new com.lowagie.text.pdf.PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(6);
        table.addCell(valueCell);
    }

    private static Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(height);
        return p;
    }

    private static String fmt(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FMT);
    }
}
