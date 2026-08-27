package com.simplehearing.assessment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.simplehearing.assessment.def.AssessmentDefinitions;
import com.simplehearing.assessment.entity.PatientAssessment;
import com.simplehearing.assessment.enums.AssessmentType;
import com.simplehearing.patient.entity.Patient;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Renders one filled-in ISAA or PRBA assessment as a PDF laid out like the clinic's paper
 * form — same item table shape, the chosen option marked per row, totals and the
 * classification band at the end. Programmatic layout via OpenPDF, matching
 * DischargePdfService's approach — no templating engine.
 */
@Service
public class AssessmentPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private static final Font TITLE_FONT   = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
    private static final Font LABEL_FONT   = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
    private static final Font VALUE_FONT   = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font ITEM_FONT    = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font COL_HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
    private static final Font OPTION_FONT  = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
    private static final Font OPTION_SELECTED_FONT = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
    private static final Font TOTAL_FONT   = new Font(Font.HELVETICA, 12, Font.BOLD);

    private static final Color ACCENT = new Color(0x37, 0x41, 0x51);
    private static final Color SELECTED_BG = new Color(0x16, 0x8a, 0x5c);
    private static final Color BAND_BG = new Color(0xf2, 0xf2, 0xf2);

    private final ObjectMapper objectMapper;

    public AssessmentPdfService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] generate(PatientAssessment assessment, Patient patient, String filledByName) {
        Map<Integer, Integer> itemScores = readItemScores(assessment.getItemScores());
        List<AssessmentDefinitions.Section> sections = AssessmentDefinitions.sectionsFor(assessment.getAssessmentType());
        int maxScore = AssessmentDefinitions.maxScoreFor(assessment.getAssessmentType());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 48, 48);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            boolean isaa = assessment.getAssessmentType() == AssessmentType.ISAA;
            doc.add(new Paragraph(isaa
                    ? "Indian Scale for Assessment of Autism (ISAA)"
                    : "Pre-Requisite Behavior (PRBA) Assessment", TITLE_FONT));
            doc.add(new Paragraph("Filled " + fmt(assessment.getAssessmentDate()), SUBTITLE_FONT));
            doc.add(spacer(10));
            doc.add(patientInfoTable(patient, assessment, filledByName));
            doc.add(spacer(14));

            if (isaa) {
                doc.add(isaaTable(sections, itemScores));
            } else {
                doc.add(prbaTable(sections, itemScores));
            }
            doc.add(spacer(14));

            doc.add(new Paragraph("Total Score: " + assessment.getTotalScore() + " / " + maxScore, TOTAL_FONT));
            if (assessment.getClassification() != null) {
                doc.add(new Paragraph("Classification: " + assessment.getClassification(), TOTAL_FONT));
            }
            doc.add(spacer(10));

            if (isaa) {
                doc.add(isaaClassificationLegend(assessment.getTotalScore()));
            }

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate assessment PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    // ── Patient info strip ─────────────────────────────────────────────────────

    private PdfPTable patientInfoTable(Patient patient, PatientAssessment assessment, String filledByName) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        infoCell(table, "Name of Child", patient.getFirstName() + " " + patient.getLastName());
        infoCell(table, "Date of Birth", patient.getDateOfBirth() != null ? fmt(patient.getDateOfBirth()) : "—");
        infoCell(table, "Gender", patient.getGender() != null ? patient.getGender().toString() : "—");
        infoCell(table, "Age at assessment", ageLabel(patient.getDateOfBirth(), assessment.getAssessmentDate()));
        infoCell(table, "Examiner", filledByName != null ? filledByName : "—");
        infoCell(table, "Assessment Date", fmt(assessment.getAssessmentDate()));
        infoCell(table, "", "");
        infoCell(table, "", "");
        return table;
    }

    private void infoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        if (!label.isEmpty()) {
            Paragraph p = new Paragraph();
            p.add(new Chunk(label + "\n", LABEL_FONT));
            p.add(new Chunk(value, VALUE_FONT));
            cell.addElement(p);
        }
        table.addCell(cell);
    }

    private String ageLabel(LocalDate dob, LocalDate assessmentDate) {
        if (dob == null) return "—";
        Period p = Period.between(dob, assessmentDate);
        return p.getYears() + "y " + p.getMonths() + "m";
    }

    // ── ISAA — uniform 5-option scale, one table for every item ────────────────

    private PdfPTable isaaTable(List<AssessmentDefinitions.Section> sections, Map<Integer, Integer> itemScores) {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 3.2f, 1f, 1f, 1f, 1f, 1f});

        colHeader(table, "#");
        colHeader(table, "Item");
        for (AssessmentDefinitions.Option opt : sections.get(0).items().get(0).options()) {
            colHeader(table, opt.label().split(" ")[0] + "\n(" + opt.score() + ")");
        }

        for (AssessmentDefinitions.Section section : sections) {
            PdfPCell sectionCell = new PdfPCell(new Phrase(section.name(), SECTION_FONT));
            sectionCell.setColspan(7);
            sectionCell.setBackgroundColor(ACCENT);
            sectionCell.setPadding(5);
            table.addCell(sectionCell);

            for (AssessmentDefinitions.Item item : section.items()) {
                Integer selected = itemScores.get(item.number());
                table.addCell(itemCell(String.valueOf(item.number())));
                table.addCell(itemCell(item.text()));
                for (AssessmentDefinitions.Option opt : item.options()) {
                    table.addCell(markCell(selected != null && selected == opt.score()));
                }
            }
        }
        return table;
    }

    // ── PRBA — each item carries its own 3 options, shown per-row ──────────────

    private PdfPTable prbaTable(List<AssessmentDefinitions.Section> sections, Map<Integer, Integer> itemScores) {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 3f, 1.5f, 1.5f, 1.5f});

        colHeader(table, "#");
        colHeader(table, "Item");
        colHeader(table, "Option A");
        colHeader(table, "Option B");
        colHeader(table, "Option C");

        for (AssessmentDefinitions.Section section : sections) {
            PdfPCell sectionCell = new PdfPCell(new Phrase(section.name(), SECTION_FONT));
            sectionCell.setColspan(5);
            sectionCell.setBackgroundColor(ACCENT);
            sectionCell.setPadding(5);
            table.addCell(sectionCell);

            int sectionTotal = 0;
            for (AssessmentDefinitions.Item item : section.items()) {
                Integer selected = itemScores.get(item.number());
                if (selected != null) sectionTotal += selected;
                table.addCell(itemCell(String.valueOf(item.number())));
                table.addCell(itemCell(item.text()));
                for (int i = 0; i < 3; i++) {
                    AssessmentDefinitions.Option opt = item.options().get(i);
                    boolean isSelected = selected != null && selected == opt.score();
                    table.addCell(optionCell(opt.label() + " (" + opt.score() + ")", isSelected));
                }
            }

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("Section Total", VALUE_FONT));
            totalLabelCell.setColspan(4);
            totalLabelCell.setPadding(5);
            totalLabelCell.setBackgroundColor(BAND_BG);
            table.addCell(totalLabelCell);
            PdfPCell totalValueCell = new PdfPCell(new Phrase(String.valueOf(sectionTotal), VALUE_FONT));
            totalValueCell.setPadding(5);
            totalValueCell.setBackgroundColor(BAND_BG);
            table.addCell(totalValueCell);
        }
        return table;
    }

    private PdfPTable isaaClassificationLegend(int total) {
        record Band(String label, int min, int max) {}
        List<Band> bands = List.of(
                new Band("No Autism (<70)", Integer.MIN_VALUE, 69),
                new Band("Mild Autism (70-106)", 70, 106),
                new Band("Moderate Autism (107-153)", 107, 153),
                new Band("Severe Autism (>153)", 154, Integer.MAX_VALUE)
        );
        PdfPTable table = new PdfPTable(bands.size());
        table.setWidthPercentage(100);
        for (Band band : bands) {
            boolean applies = total >= band.min() && total <= band.max();
            PdfPCell cell = new PdfPCell(new Phrase(band.label(), applies ? OPTION_SELECTED_FONT : OPTION_FONT));
            cell.setPadding(6);
            cell.setBackgroundColor(applies ? SELECTED_BG : BAND_BG);
            table.addCell(cell);
        }
        return table;
    }

    // ── Cell helpers ─────────────────────────────────────────────────────────

    private void colHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, COL_HEADER_FONT));
        cell.setBackgroundColor(ACCENT);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private PdfPCell itemCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, ITEM_FONT));
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell markCell(boolean selected) {
        PdfPCell cell = new PdfPCell(new Phrase(selected ? "✓" : "", VALUE_FONT));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (selected) cell.setBackgroundColor(SELECTED_BG);
        return cell;
    }

    private PdfPCell optionCell(String text, boolean selected) {
        PdfPCell cell = new PdfPCell(new Phrase(text, selected ? OPTION_SELECTED_FONT : OPTION_FONT));
        cell.setPadding(4);
        if (selected) cell.setBackgroundColor(SELECTED_BG);
        return cell;
    }

    private Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(height);
        return p;
    }

    private String fmt(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FMT);
    }

    private Map<Integer, Integer> readItemScores(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<Integer, Integer>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode item scores", e);
        }
    }
}
