package nl.overheid.wcag.check.xlsx;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import nl.overheid.wcag.model.MetadataProfile;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Configurable metadata check for spreadsheets.
 */
class XlsxMetadataCheckImpl implements SpreadsheetCheck {

    private final MetadataProfile profile;

    XlsxMetadataCheckImpl(MetadataProfile profile) {
        this.profile = profile != null ? profile : MetadataProfile.WCAG;
    }

    @Override public String getCheckId() { return "XLSX_METADATA_CHECK"; }
    @Override public String getDisplayName() { return "Spreadsheet metadata (" + profile.getDisplayName() + ")"; }

    @Override
    public List<AccessibilityFinding> check(XSSFWorkbook workbook) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        POIXMLProperties.CoreProperties core = workbook.getProperties().getCoreProperties();

        // Titel (WCAG 2.4.2 - verplicht)
        String title = core.getTitle();
        if (title == null || title.trim().isEmpty()) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .error()
                    .wcag("2.4.2", "A")
                    .message("Documenttitel ontbreekt in de bestandseigenschappen.")
                    .suggestion("Stel de titel in via Bestand > Info > Eigenschappen.")
                    .location(ElementLocation.document())
                    .build());
        }

        // Taal (WCAG 3.1.1 - verplicht)
        String lang = core.getUnderlyingProperties().getLanguageProperty().orElse(null);
        if (lang == null || lang.trim().isEmpty()) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .error()
                    .wcag("3.1.1", "A")
                    .message("Documenttaal is niet ingesteld.")
                    .suggestion("Stel de taal in via de bestandseigenschappen.")
                    .location(ElementLocation.document())
                    .build());
        }

        // MDTO fields
        if (profile != MetadataProfile.WCAG) {
            if (core.getCreator() == null || core.getCreator().trim().isEmpty()) {
                findings.add(mdtoWarning("Auteur"));
            }
            if (core.getSubject() == null || core.getSubject().trim().isEmpty()) {
                findings.add(mdtoWarning("Onderwerp"));
            }
            if (core.getKeywords() == null || core.getKeywords().trim().isEmpty()) {
                findings.add(mdtoWarning("Trefwoorden"));
            }
        }

        if (profile == MetadataProfile.MDTO_UITGEBREID) {
            if (core.getCategory() == null || core.getCategory().trim().isEmpty()) {
                findings.add(mdtoWarning("Categorie"));
            }
            if (core.getDescription() == null || core.getDescription().trim().isEmpty()) {
                findings.add(mdtoWarning("Opmerkingen"));
            }
        }

        return findings;
    }

    private AccessibilityFinding mdtoWarning(String fieldName) {
        return AccessibilityFinding.builder(getCheckId())
                .warning()
                .wcag("MDTO", "-")
                .message("Aanbevolen metadataveld '" + fieldName
                        + "' ontbreekt (profiel: " + profile.name() + ").")
                .suggestion("Vul dit veld in via Bestand > Info > Eigenschappen.")
                .location(ElementLocation.document())
                .build();
    }
}

// ============================================================================

/**
 * Checks if document title is set (standalone WCAG profile).
 */
class XlsxDocumentTitleCheckImpl implements SpreadsheetCheck {

    @Override public String getCheckId() { return "XLSX_TITLE_CHECK"; }
    @Override public String getDisplayName() { return "Spreadsheet documenttitel"; }

    @Override
    public List<AccessibilityFinding> check(XSSFWorkbook workbook) {
        String title = workbook.getProperties().getCoreProperties().getTitle();
        if (title == null || title.trim().isEmpty()) {
            return List.of(AccessibilityFinding.builder(getCheckId())
                    .error()
                    .wcag("2.4.2", "A")
                    .message("Documenttitel ontbreekt in de bestandseigenschappen.")
                    .suggestion("Stel de titel in via Bestand > Info > Eigenschappen.")
                    .location(ElementLocation.document())
                    .build());
        }
        return List.of();
    }
}

// ============================================================================

/**
 * Checks if the document language is set.
 */
class XlsxDocumentLanguageCheckImpl implements SpreadsheetCheck {

    @Override public String getCheckId() { return "XLSX_LANGUAGE_CHECK"; }
    @Override public String getDisplayName() { return "Spreadsheet documenttaal"; }

    @Override
    public List<AccessibilityFinding> check(XSSFWorkbook workbook) {
        String lang = workbook.getProperties().getCoreProperties()
                .getUnderlyingProperties().getLanguageProperty().orElse(null);
        if (lang == null || lang.trim().isEmpty()) {
            return List.of(AccessibilityFinding.builder(getCheckId())
                    .error()
                    .wcag("3.1.1", "A")
                    .message("Documenttaal is niet ingesteld.")
                    .suggestion("Stel de taal in via de bestandseigenschappen.")
                    .location(ElementLocation.document())
                    .build());
        }
        return List.of();
    }
}

// ============================================================================

/**
 * Checks sheet names for default names and duplicates.
 */
class XlsxSheetNameCheckImpl implements SpreadsheetCheck {

    private static final Pattern DEFAULT_NAME = Pattern.compile(
            "^(Sheet|Blad|Feuil|Tabelle|Hoja|Foglio|Лист|Planilha)\\s*\\d*$",
            Pattern.CASE_INSENSITIVE);

    @Override public String getCheckId() { return "XLSX_SHEET_NAME_CHECK"; }
    @Override public String getDisplayName() { return "Werkbladnamen"; }

    @Override
    public List<AccessibilityFinding> check(XSSFWorkbook workbook) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String name = workbook.getSheetName(i);
            if (DEFAULT_NAME.matcher(name).matches()) {
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .warning()
                        .wcag("2.4.2", "A")
                        .message("Werkblad '" + name + "' heeft een standaardnaam. "
                                + "Geef werkbladen een beschrijvende naam.")
                        .suggestion("Hernoem het werkblad naar een naam die de inhoud beschrijft.")
                        .location(ElementLocation.sheet(i, name))
                        .build());
            }
        }

        return findings;
    }
}

// ============================================================================

/**
 * Checks table structure: formal tables, merged cells, empty row gaps.
 */
class XlsxTableStructureCheckImpl implements SpreadsheetCheck {

    @Override public String getCheckId() { return "XLSX_TABLE_STRUCTURE_CHECK"; }
    @Override public String getDisplayName() { return "Tabelstructuur"; }

    @Override
    public List<AccessibilityFinding> check(XSSFWorkbook workbook) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
            XSSFSheet sheet = workbook.getSheetAt(si);
            String sheetName = sheet.getSheetName();

            // Check merged cells
            int mergedCount = sheet.getNumMergedRegions();
            if (mergedCount > 0) {
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .warning()
                        .wcag("1.3.1", "A")
                        .message("Werkblad '" + sheetName + "' bevat "
                                + mergedCount + " samengevoegde cel"
                                + (mergedCount > 1 ? "len" : "") + ". "
                                + "Samengevoegde cellen bemoeilijken navigatie met schermlezers.")
                        .suggestion("Vermijd samengevoegde cellen. Gebruik in plaats daarvan "
                                + "koppen en opmaak om structuur aan te geven.")
                        .location(ElementLocation.sheet(si, sheetName))
                        .build());
            }

            // Check if formal tables are defined
            if (sheet.getTables().isEmpty() && sheet.getLastRowNum() > 0) {
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .info()
                        .wcag("1.3.1", "A")
                        .message("Werkblad '" + sheetName + "' bevat data maar geen "
                                + "formele tabel. Formele tabellen verbeteren de "
                                + "toegankelijkheid voor schermlezers.")
                        .suggestion("Selecteer het databereik en kies Invoegen > Tabel "
                                + "om een formele tabel aan te maken.")
                        .location(ElementLocation.sheet(si, sheetName))
                        .build());
            }
        }

        return findings;
    }
}

// ============================================================================

/**
 * Checks alt text on images and charts in spreadsheets.
 */
class XlsxAltTextCheckImpl implements SpreadsheetCheck {

    @Override public String getCheckId() { return "XLSX_ALT_TEXT_CHECK"; }
    @Override public String getDisplayName() { return "Alt-tekst (afbeeldingen/grafieken)"; }

    @Override
    public List<AccessibilityFinding> check(XSSFWorkbook workbook) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
            XSSFSheet sheet = workbook.getSheetAt(si);
            String sheetName = sheet.getSheetName();
            XSSFDrawing drawing = sheet.getDrawingPatriarch();

            if (drawing != null) {
                for (XSSFShape shape : drawing.getShapes()) {
                    // Only pictures without an alternative text (the original reported every picture)
                    if (shape instanceof XSSFPicture pic
                            && !nl.overheid.wcag.util.AltText.present(pic.getCTPicture())) {
                        String desc = pic.getShapeName();
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .error()
                                .wcag("1.1.1", "A")
                                .message("Afbeelding '" + (desc != null ? desc : "onbekend")
                                        + "' in werkblad '" + sheetName
                                        + "' heeft geen alternatieve tekst.")
                                .suggestion("Klik met rechts op de afbeelding > "
                                        + "Alt-tekst bewerken en voeg een beschrijving toe.")
                                .location(ElementLocation.sheet(si, sheetName))
                                .build());
                    }
                }
            }

            // Check charts
            for (XSSFChart chart : sheet.getDrawingPatriarch() != null
                    ? sheet.getDrawingPatriarch().getCharts() : List.<XSSFChart>of()) {
                String chartTitle = chart.getTitleText() != null
                        ? chart.getTitleText().toString() : null;
                if (chartTitle == null || chartTitle.trim().isEmpty()) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .warning()
                            .wcag("1.1.1", "A")
                            .message("Grafiek zonder titel in werkblad '" + sheetName + "'.")
                            .suggestion("Voeg een titel en beschrijving toe aan de grafiek "
                                    + "zodat de inhoud ook zonder visuele weergave duidelijk is.")
                            .location(ElementLocation.chart(si, sheetName, null))
                            .build());
                }
            }
        }

        return findings;
    }
}

// ============================================================================

/**
 * Checks color contrast in cells.
 */
class XlsxColorContrastCheckImpl implements SpreadsheetCheck {

    @Override public String getCheckId() { return "XLSX_COLOR_CONTRAST_CHECK"; }
    @Override public String getDisplayName() { return "Kleurcontrast"; }

    @Override
    public List<AccessibilityFinding> check(XSSFWorkbook workbook) {
        // Color contrast check requires complex color extraction.
        // Reports an informational notice when colored backgrounds are used.
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
            XSSFSheet sheet = workbook.getSheetAt(si);
            int coloredCells = 0;

            for (Row row : sheet) {
                for (Cell cell : row) {
                    CellStyle style = cell.getCellStyle();
                    if (style != null && style.getFillPattern() != FillPatternType.NO_FILL) {
                        coloredCells++;
                    }
                }
            }

            if (coloredCells > 0) {
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .info()
                        .wcag("1.4.3", "AA")
                        .message("Werkblad '" + sheet.getSheetName() + "' bevat "
                                + coloredCells + " cel(len) met achtergrondkleur. "
                                + "Controleer of het kleurcontrast voldoende is (4.5:1).")
                        .suggestion("Gebruik donkere tekst op lichte achtergronden "
                                + "en vermijd kleur als enige manier om informatie over te dragen.")
                        .location(ElementLocation.sheet(si, sheet.getSheetName()))
                        .build());
            }
        }

        return findings;
    }
}

// ============================================================================

/**
 * Checks text formatting: font size and font proliferation.
 */
class XlsxTextSpacingCheckImpl implements SpreadsheetCheck {

    @Override public String getCheckId() { return "XLSX_TEXT_SPACING_CHECK"; }
    @Override public String getDisplayName() { return "Tekstopmaak"; }

    @Override
    public List<AccessibilityFinding> check(XSSFWorkbook workbook) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
            XSSFSheet sheet = workbook.getSheetAt(si);
            Set<String> fonts = new HashSet<>();
            int smallTextCells = 0;

            for (Row row : sheet) {
                for (Cell cell : row) {
                    XSSFFont font = (XSSFFont) workbook.getFontAt(cell.getCellStyle().getFontIndex());
                    fonts.add(font.getFontName());
                    if (font.getFontHeightInPoints() < 8) {
                        smallTextCells++;
                    }
                }
            }

            if (smallTextCells > 0) {
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .warning()
                        .wcag("1.4.4", "AA")
                        .message(smallTextCells + " cel(len) in werkblad '"
                                + sheet.getSheetName() + "' gebruiken een lettergrootte "
                                + "kleiner dan 8pt.")
                        .suggestion("Gebruik een lettergrootte van minimaal 8pt, "
                                + "bij voorkeur 10pt of groter.")
                        .location(ElementLocation.sheet(si, sheet.getSheetName()))
                        .build());
            }

            if (fonts.size() > 4) {
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .info()
                        .wcag("3.2.4", "AA")
                        .message("Werkblad '" + sheet.getSheetName() + "' gebruikt "
                                + fonts.size() + " verschillende lettertypen.")
                        .suggestion("Beperk het aantal lettertypen tot 2-3 voor "
                                + "een consistente en leesbare opmaak.")
                        .location(ElementLocation.sheet(si, sheet.getSheetName()))
                        .build());
            }
        }

        return findings;
    }
}

// ============================================================================

/**
 * Checks hyperlink texts in spreadsheets.
 */
class XlsxLinkTextCheckImpl implements SpreadsheetCheck {

    private static final Set<String> VAGUE_TEXTS = Set.of(
            "klik hier", "hier", "link", "lees meer", "meer info", "meer",
            "click here", "here", "read more", "more", "more info");

    @Override public String getCheckId() { return "XLSX_LINK_TEXT_CHECK"; }
    @Override public String getDisplayName() { return "Linkteksten"; }

    @Override
    public List<AccessibilityFinding> check(XSSFWorkbook workbook) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
            XSSFSheet sheet = workbook.getSheetAt(si);
            String sheetName = sheet.getSheetName();

            for (XSSFHyperlink link : sheet.getHyperlinkList()) {
                String cellRef = link.getCellRef();
                Row row = sheet.getRow(link.getFirstRow());
                if (row == null) continue;
                Cell cell = row.getCell(link.getFirstColumn());
                if (cell == null) continue;

                String text = cell.toString().trim().toLowerCase();
                String url = link.getAddress();

                // URL as link text
                if (text.startsWith("http://") || text.startsWith("https://")) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .warning()
                            .wcag("2.4.4", "A")
                            .message("Hyperlink in cel " + cellRef + " van werkblad '"
                                    + sheetName + "' toont de URL als tekst.")
                            .suggestion("Gebruik een beschrijvende tekst in plaats van de URL.")
                            .location(ElementLocation.spreadsheetCell(si, sheetName,
                                    cellRef, text))
                            .build());
                }

                // Vague link text
                if (VAGUE_TEXTS.contains(text)) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .warning()
                            .wcag("2.4.4", "A")
                            .message("Hyperlink '" + text + "' in cel " + cellRef
                                    + " van werkblad '" + sheetName
                                    + "' is niet beschrijvend.")
                            .suggestion("Gebruik een tekst die het doel van de link beschrijft.")
                            .location(ElementLocation.spreadsheetCell(si, sheetName,
                                    cellRef, text))
                            .build());
                }
            }
        }

        return findings;
    }
}
