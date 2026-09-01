package com.simplehearing.assessment.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.simplehearing.assessment.entity.*;
import com.simplehearing.assessment.repository.*;
import com.simplehearing.patient.entity.Patient;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renders one filled-in assessment as a PDF — patient info strip, then each category with
 * its items and the answer given (selected option(s) marked, or free text/file link),
 * followed by the total score and classification band if the definition is scored.
 * Driven entirely by the assessment_definitions/categories/items/options data, so this
 * works uniformly for every assessment type without per-type layout code.
 */
@Service
public class AssessmentPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private static final Font TITLE_FONT   = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
    private static final Font LABEL_FONT   = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
    private static final Font VALUE_FONT   = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font ITEM_FONT    = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font ANSWER_FONT  = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(0x16, 0x6a, 0x48));
    private static final Font TOTAL_FONT   = new Font(Font.HELVETICA, 12, Font.BOLD);

    private static final Color ACCENT = new Color(0x37, 0x41, 0x51);

    private final AssessmentCategoryRepository categoryRepository;
    private final AssessmentItemRepository itemRepository;
    private final AssessmentItemOptionRepository optionRepository;
    private final AssessmentResponseRepository responseRepository;

    public AssessmentPdfService(AssessmentCategoryRepository categoryRepository,
                                 AssessmentItemRepository itemRepository,
                                 AssessmentItemOptionRepository optionRepository,
                                 AssessmentResponseRepository responseRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.optionRepository = optionRepository;
        this.responseRepository = responseRepository;
    }

    public byte[] generate(PatientAssessment assessment, AssessmentDefinition definition, Patient patient, String filledByName) {
        List<AssessmentCategory> categories = categoryRepository.findByDefinitionIdOrderByDisplayOrder(definition.getId());
        List<UUID> categoryIds = categories.stream().map(AssessmentCategory::getId).toList();
        List<AssessmentItem> items = itemRepository.findByCategoryIdIn(categoryIds);
        items.sort(Comparator.comparingInt(AssessmentItem::getDisplayOrder));
        Map<UUID, List<AssessmentItem>> itemsByCategory = new LinkedHashMap<>();
        for (AssessmentCategory c : categories) itemsByCategory.put(c.getId(), new ArrayList<>());
        for (AssessmentItem i : items) itemsByCategory.get(i.getCategoryId()).add(i);

        List<UUID> itemIds = items.stream().map(AssessmentItem::getId).toList();
        Map<UUID, AssessmentItemOption> optionsById = new HashMap<>();
        for (AssessmentItemOption o : optionRepository.findByItemIdIn(itemIds)) optionsById.put(o.getId(), o);

        Map<UUID, AssessmentResponse> responseByItem = new HashMap<>();
        for (AssessmentResponse r : responseRepository.findByPatientAssessmentId(assessment.getId())) {
            responseByItem.put(r.getItemId(), r);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 48, 48);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph(definition.getName(), TITLE_FONT));
            doc.add(new Paragraph("Filled " + fmt(assessment.getAssessmentDate()), SUBTITLE_FONT));
            doc.add(spacer(10));
            doc.add(patientInfoTable(patient, assessment, filledByName));
            doc.add(spacer(14));

            for (AssessmentCategory category : categories) {
                doc.add(categoryTable(category, itemsByCategory.get(category.getId()), optionsById, responseByItem));
                doc.add(spacer(8));
            }

            if (assessment.getTotalScore() != null) {
                doc.add(new Paragraph("Total Score: " + assessment.getTotalScore(), TOTAL_FONT));
            }
            if (assessment.getClassification() != null) {
                doc.add(new Paragraph("Classification: " + assessment.getClassification(), TOTAL_FONT));
            }
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate assessment PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

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

    private PdfPTable categoryTable(AssessmentCategory category, List<AssessmentItem> items,
                                     Map<UUID, AssessmentItemOption> optionsById, Map<UUID, AssessmentResponse> responseByItem) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 3f});

        PdfPCell header = new PdfPCell(new Phrase(category.getName(), SECTION_FONT));
        header.setColspan(2);
        header.setBackgroundColor(ACCENT);
        header.setPadding(5);
        table.addCell(header);

        for (AssessmentItem item : items) {
            table.addCell(itemCell(String.valueOf(item.getItemNumber())));
            PdfPCell answerCell = new PdfPCell();
            answerCell.setPadding(4);
            Paragraph p = new Paragraph();
            p.add(new Chunk(item.getText() + "\n", ITEM_FONT));
            p.add(new Chunk(answerText(item, optionsById, responseByItem.get(item.getId())), ANSWER_FONT));
            answerCell.addElement(p);
            table.addCell(answerCell);
        }
        return table;
    }

    private String answerText(AssessmentItem item, Map<UUID, AssessmentItemOption> optionsById, AssessmentResponse response) {
        if (response == null) return "(no answer)";
        return switch (item.getItemType()) {
            case SINGLE_SELECT -> {
                AssessmentItemOption opt = response.getSelectedOptionId() != null ? optionsById.get(response.getSelectedOptionId()) : null;
                yield opt != null ? opt.getLabel() : "(no answer)";
            }
            case MULTI_SELECT -> {
                if (response.getTextValue() == null || response.getTextValue().isBlank()) yield "(no answer)";
                List<String> labels = new ArrayList<>();
                for (String idStr : response.getTextValue().split(",")) {
                    AssessmentItemOption opt = optionsById.get(UUID.fromString(idStr));
                    if (opt != null) labels.add(opt.getLabel());
                }
                yield String.join(", ", labels);
            }
            case TEXT, FILE -> response.getTextValue() != null && !response.getTextValue().isBlank() ? response.getTextValue() : "(no answer)";
        };
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

    private PdfPCell itemCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, ITEM_FONT));
        cell.setPadding(4);
        return cell;
    }

    private String ageLabel(LocalDate dob, LocalDate assessmentDate) {
        if (dob == null) return "—";
        Period p = Period.between(dob, assessmentDate);
        return p.getYears() + "y " + p.getMonths() + "m";
    }

    private Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(height);
        return p;
    }

    private String fmt(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FMT);
    }
}
