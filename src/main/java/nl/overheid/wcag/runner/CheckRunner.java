package nl.overheid.wcag.runner;

import nl.overheid.wcag.check.*;
import nl.overheid.wcag.check.xlsx.*;
import nl.overheid.wcag.check.pptx.*;
import nl.overheid.wcag.model.*;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Central orchestrator for WCAG checks across all document types.
 *
 * <p>Selects the appropriate set of checks based on document type and runs
 * them. The metadata profile determines which metadata fields are
 * checked (WCAG, MDTO_BASIS, MDTO_UITGEBREID).
 *
 * <p>Gebruik:
 * <pre>
 *   CheckRunner runner = new CheckRunner(MetadataProfile.MDTO_BASIS);
 *   AccessibilityReport report = runner.check(inputStream, "document.xlsx");
 * </pre>
 */
public class CheckRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CheckRunner.class);

    private final MetadataProfile metadataProfile;
    private final List<AccessibilityCheck> docxChecks;
    private final List<SpreadsheetCheck> xlsxChecks;
    private final List<PresentationCheck> pptxChecks;

    /**
     * Creates a CheckRunner with the default WCAG metadata profile.
     */
    public CheckRunner() {
        this(MetadataProfile.WCAG);
    }

    /**
     * Creates a CheckRunner with the specified metadata profile.
     */
    public CheckRunner(MetadataProfile metadataProfile) {
        this.metadataProfile = metadataProfile != null ? metadataProfile : MetadataProfile.WCAG;
        this.docxChecks = buildDocxChecks(this.metadataProfile);
        this.xlsxChecks = buildXlsxChecks(this.metadataProfile);
        this.pptxChecks = buildPptxChecks(this.metadataProfile);

        LOG.info("CheckRunner geïnitialiseerd [profiel={}, docx={}, xlsx={}, pptx={}]",
                this.metadataProfile.getDisplayName(),
                docxChecks.size(), xlsxChecks.size(), pptxChecks.size());
    }

    // ========================================================================
    // Check list construction per document type
    // ========================================================================

    private static List<AccessibilityCheck> buildDocxChecks(MetadataProfile profile) {
        List<AccessibilityCheck> checks = new ArrayList<>();

        // Metadata (profile-dependent)
        if (profile == MetadataProfile.WCAG) {
            checks.add(new DocumentTitleCheck());
            checks.add(new DocumentLanguageCheck());
        } else {
            checks.add(new DocumentMetadataCheck(profile));
        }

        // Structure & content
        checks.add(new AltTextCheck());
        checks.add(new HeadingStructureCheck());
        checks.add(new TableHeaderCheck());
        checks.add(new ListStructureCheck());
        checks.add(new ReadingOrderCheck());
        checks.add(new TableOfContentsCheck());

        // Visual
        checks.add(new ColorContrastCheck());
        checks.add(new NonTextContrastCheck());
        checks.add(new UseOfColorCheck());
        checks.add(new SensoryCharacteristicsCheck());
        checks.add(new TextSpacingCheck());
        checks.add(new ImagesOfTextCheck());

        // Interaction
        checks.add(new LinkTextCheck());
        checks.add(new FormFieldCheck());
        checks.add(new ConsistentIdentificationCheck());

        // Technical
        checks.add(new DocumentParsingCheck());

        return checks;
    }

    private static List<SpreadsheetCheck> buildXlsxChecks(MetadataProfile profile) {
        List<SpreadsheetCheck> checks = new ArrayList<>();

        if (profile == MetadataProfile.WCAG) {
            checks.add(XlsxCheckFactory.documentTitleCheck());
            checks.add(XlsxCheckFactory.documentLanguageCheck());
        } else {
            checks.add(XlsxCheckFactory.metadataCheck(profile));
        }

        checks.add(XlsxCheckFactory.sheetNameCheck());
        checks.add(XlsxCheckFactory.tableStructureCheck());
        checks.add(XlsxCheckFactory.altTextCheck());
        checks.add(XlsxCheckFactory.colorContrastCheck());
        checks.add(XlsxCheckFactory.textSpacingCheck());
        checks.add(XlsxCheckFactory.linkTextCheck());

        return checks;
    }

    private static List<PresentationCheck> buildPptxChecks(MetadataProfile profile) {
        List<PresentationCheck> checks = new ArrayList<>();

        if (profile == MetadataProfile.WCAG) {
            checks.add(PptxCheckFactory.documentTitleCheck());
            checks.add(PptxCheckFactory.documentLanguageCheck());
        } else {
            checks.add(PptxCheckFactory.metadataCheck(profile));
        }

        checks.add(PptxCheckFactory.slideTitleCheck());
        checks.add(PptxCheckFactory.altTextCheck());
        checks.add(PptxCheckFactory.readingOrderCheck());
        checks.add(PptxCheckFactory.colorContrastCheck());
        checks.add(PptxCheckFactory.tableCheck());
        checks.add(PptxCheckFactory.linkTextCheck());

        return checks;
    }

    // ========================================================================
    // Document checking
    // ========================================================================

    /**
     * Performs WCAG check on a document.
     *
     * @param inputStream the document content
     * @param filename    the filename (for type detection)
     * @return the accessibility report
     */
    public AccessibilityReport check(InputStream inputStream, String filename) throws IOException {
        DocumentType docType = DocumentType.fromFilename(filename);
        LOG.info("Start WCAG-controle: '{}' (type: {}, profiel: {})",
                filename, docType, metadataProfile);

        long start = System.currentTimeMillis();

        AccessibilityReport report;
        switch (docType) {
            case DOCX: report = checkDocx(inputStream, filename); break;
            case XLSX: report = checkXlsx(inputStream, filename); break;
            case PPTX: report = checkPptx(inputStream, filename); break;
            default: throw new IllegalArgumentException("Onbekend documenttype: " + docType);
        }

        long duration = System.currentTimeMillis() - start;
        LOG.info("Controle voltooid '{}': {} bevindingen in {}ms (E:{} W:{} I:{})",
                filename, report.getFindings().size(), duration,
                report.getSummary().getErrors(),
                report.getSummary().getWarnings(),
                report.getSummary().getInfos());

        return report;
    }

    private AccessibilityReport checkDocx(InputStream inputStream, String filename) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            DocumentContext context = new DocumentContext(document);
            AccessibilityReport report = new AccessibilityReport();
            report.setDocumentId(filename);

            for (AccessibilityCheck check : docxChecks) {
                try {
                    report.addAllFindings(check.check(document, context));
                } catch (Exception e) {
                    LOG.error("Fout bij check '{}': {}", check.getCheckId(), e.getMessage());
                    report.addFinding(errorFinding(check.getCheckId(), e));
                }
            }

            report.setDocumentType(DocumentType.DOCX);
            report.setMetadataProfile(metadataProfile);
            report.buildSummary();
            return report;
        }
    }

    private AccessibilityReport checkXlsx(InputStream inputStream, String filename) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            AccessibilityReport report = new AccessibilityReport();
            report.setDocumentId(filename);

            for (SpreadsheetCheck check : xlsxChecks) {
                try {
                    report.addAllFindings(check.check(workbook));
                } catch (Exception e) {
                    LOG.error("Fout bij check '{}': {}", check.getCheckId(), e.getMessage());
                    report.addFinding(errorFinding(check.getCheckId(), e));
                }
            }

            report.setDocumentType(DocumentType.XLSX);
            report.setMetadataProfile(metadataProfile);
            report.buildSummary();
            return report;
        }
    }

    private AccessibilityReport checkPptx(InputStream inputStream, String filename) throws IOException {
        try (XMLSlideShow slideShow = new XMLSlideShow(inputStream)) {
            AccessibilityReport report = new AccessibilityReport();
            report.setDocumentId(filename);

            for (PresentationCheck check : pptxChecks) {
                try {
                    report.addAllFindings(check.check(slideShow));
                } catch (Exception e) {
                    LOG.error("Fout bij check '{}': {}", check.getCheckId(), e.getMessage());
                    report.addFinding(errorFinding(check.getCheckId(), e));
                }
            }

            report.setDocumentType(DocumentType.PPTX);
            report.setMetadataProfile(metadataProfile);
            report.buildSummary();
            return report;
        }
    }

    private AccessibilityFinding errorFinding(String checkId, Exception e) {
        return AccessibilityFinding.builder(checkId)
                .error().wcag("-", "-")
                .message("Interne fout bij controle: " + e.getMessage())
                .location(ElementLocation.document())
                .build();
    }

    // ========================================================================
    // Information methods
    // ========================================================================

    public MetadataProfile getMetadataProfile() { return metadataProfile; }

    public int getCheckCount(DocumentType docType) {
        switch (docType) {
            case DOCX: return docxChecks.size();
            case XLSX: return xlsxChecks.size();
            case PPTX: return pptxChecks.size();
            default: return 0;
        }
    }

    public int getTotalCheckCount() {
        return docxChecks.size() + xlsxChecks.size() + pptxChecks.size();
    }

    public List<String> getCheckNames(DocumentType docType) {
        List<String> names = new ArrayList<>();
        switch (docType) {
            case DOCX: docxChecks.forEach(c -> names.add(c.getCheckId())); break;
            case XLSX: xlsxChecks.forEach(c -> names.add(c.getCheckId())); break;
            case PPTX: pptxChecks.forEach(c -> names.add(c.getCheckId())); break;
        }
        return names;
    }

    public String getConfigSummary() {
        return String.format("CheckRunner [profiel=%s, docx=%d, xlsx=%d, pptx=%d]",
                metadataProfile.getDisplayName(),
                docxChecks.size(), xlsxChecks.size(), pptxChecks.size());
    }
}
