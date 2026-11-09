package nl.overheid.wcag.check.pptx;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import nl.overheid.wcag.model.MetadataProfile;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xslf.usermodel.*;

import java.util.*;

/**
 * Configurable metadata check for presentations.
 */
class PptxMetadataCheckImpl implements PresentationCheck {

    private final MetadataProfile profile;

    PptxMetadataCheckImpl(MetadataProfile profile) {
        this.profile = profile != null ? profile : MetadataProfile.WCAG;
    }

    @Override public String getCheckId() { return "PPTX_METADATA_CHECK"; }
    @Override public String getDisplayName() { return "Presentatie metadata (" + profile.getDisplayName() + ")"; }

    @Override
    public List<AccessibilityFinding> check(XMLSlideShow slideShow) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        POIXMLProperties.CoreProperties core = slideShow.getProperties().getCoreProperties();

        String title = core.getTitle();
        if (title == null || title.trim().isEmpty()) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .error().wcag("2.4.2", "A")
                    .message("Presentatietitel ontbreekt in de bestandseigenschappen.")
                    .suggestion("Stel de titel in via Bestand > Info > Eigenschappen.")
                    .location(ElementLocation.document()).build());
        }

        String lang = core.getUnderlyingProperties().getLanguageProperty().orElse(null);
        if (lang == null || lang.trim().isEmpty()) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .error().wcag("3.1.1", "A")
                    .message("Presentatietaal is niet ingesteld.")
                    .suggestion("Stel de taal in via de bestandseigenschappen.")
                    .location(ElementLocation.document()).build());
        }

        if (profile != MetadataProfile.WCAG) {
            checkMdtoField(findings, core.getCreator(), "Auteur");
            checkMdtoField(findings, core.getSubject(), "Onderwerp");
            checkMdtoField(findings, core.getKeywords(), "Trefwoorden");
        }
        if (profile == MetadataProfile.MDTO_UITGEBREID) {
            checkMdtoField(findings, core.getCategory(), "Categorie");
            checkMdtoField(findings, core.getDescription(), "Opmerkingen");
        }

        return findings;
    }

    private void checkMdtoField(List<AccessibilityFinding> findings, String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .warning().wcag("MDTO", "-")
                    .message("Aanbevolen metadataveld '" + name + "' ontbreekt (profiel: " + profile.name() + ").")
                    .suggestion("Vul dit veld in via Bestand > Info > Eigenschappen.")
                    .location(ElementLocation.document()).build());
        }
    }
}

// ============================================================================

class PptxDocumentTitleCheckImpl implements PresentationCheck {
    @Override public String getCheckId() { return "PPTX_TITLE_CHECK"; }
    @Override public String getDisplayName() { return "Presentatietitel"; }

    @Override
    public List<AccessibilityFinding> check(XMLSlideShow slideShow) {
        String title = slideShow.getProperties().getCoreProperties().getTitle();
        if (title == null || title.trim().isEmpty()) {
            return List.of(AccessibilityFinding.builder(getCheckId())
                    .error().wcag("2.4.2", "A")
                    .message("Presentatietitel ontbreekt in de bestandseigenschappen.")
                    .suggestion("Stel de titel in via Bestand > Info > Eigenschappen.")
                    .location(ElementLocation.document()).build());
        }
        return List.of();
    }
}

// ============================================================================

class PptxDocumentLanguageCheckImpl implements PresentationCheck {
    @Override public String getCheckId() { return "PPTX_LANGUAGE_CHECK"; }
    @Override public String getDisplayName() { return "Presentatietaal"; }

    @Override
    public List<AccessibilityFinding> check(XMLSlideShow slideShow) {
        String lang = slideShow.getProperties().getCoreProperties()
                .getUnderlyingProperties().getLanguageProperty().orElse(null);
        if (lang == null || lang.trim().isEmpty()) {
            return List.of(AccessibilityFinding.builder(getCheckId())
                    .error().wcag("3.1.1", "A")
                    .message("Presentatietaal is niet ingesteld.")
                    .suggestion("Stel de taal in via de bestandseigenschappen.")
                    .location(ElementLocation.document()).build());
        }
        return List.of();
    }
}

// ============================================================================

/**
 * Checks if every slide has a title and detects duplicates.
 */
class PptxSlideTitleCheckImpl implements PresentationCheck {
    @Override public String getCheckId() { return "PPTX_SLIDE_TITLE_CHECK"; }
    @Override public String getDisplayName() { return "Diatitels"; }

    @Override
    public List<AccessibilityFinding> check(XMLSlideShow slideShow) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        Map<String, Integer> titleCounts = new HashMap<>();
        List<XSLFSlide> slides = slideShow.getSlides();

        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            int slideNum = i + 1;
            String title = extractSlideTitle(slide);

            if (title == null || title.trim().isEmpty()) {
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .error().wcag("2.4.2", "A")
                        .message("Dia " + slideNum + " heeft geen titel.")
                        .suggestion("Voeg een titelplaceholder toe via Beeld > Diamodel "
                                + "of typ een titel in het titelvak.")
                        .location(ElementLocation.slide(slideNum, null)).build());
            } else {
                titleCounts.merge(title.trim().toLowerCase(), 1, Integer::sum);
            }
        }

        // Detect duplicates
        titleCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .forEach(e -> findings.add(AccessibilityFinding.builder(getCheckId())
                        .warning().wcag("2.4.2", "A")
                        .message("Diatitel '" + e.getKey() + "' komt " + e.getValue()
                                + " keer voor. Unieke titels verbeteren de navigatie.")
                        .suggestion("Geef elke dia een unieke, beschrijvende titel.")
                        .location(ElementLocation.document()).build()));

        return findings;
    }

    private String extractSlideTitle(XSLFSlide slide) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape) {
                XSLFTextShape textShape = (XSLFTextShape) shape;
                if (textShape.isPlaceholder()) {
                    // Check for title placeholder types
                    org.apache.poi.sl.usermodel.Placeholder ph = textShape.getPlaceholder();
                    if (ph == org.apache.poi.sl.usermodel.Placeholder.TITLE
                            || ph == org.apache.poi.sl.usermodel.Placeholder.CENTERED_TITLE) {
                        return textShape.getText();
                    }
                }
            }
        }
        return null;
    }
}

// ============================================================================

/**
 * Checks alt text on all shapes in the presentation.
 */
class PptxAltTextCheckImpl implements PresentationCheck {
    @Override public String getCheckId() { return "PPTX_ALT_TEXT_CHECK"; }
    @Override public String getDisplayName() { return "Alt-tekst"; }

    @Override
    public List<AccessibilityFinding> check(XMLSlideShow slideShow) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        List<XSLFSlide> slides = slideShow.getSlides();

        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            int slideNum = i + 1;
            String slideTitle = null;

            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFPictureShape) {
                    XSLFPictureShape pic = (XSLFPictureShape) shape;
                    if (!nl.overheid.wcag.util.AltText.present(pic.getXmlObject())) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .error().wcag("1.1.1", "A")
                                .message("Afbeelding '" + pic.getShapeName()
                                        + "' op dia " + slideNum + " mist een alternatieve tekst.")
                                .suggestion("Klik met rechts op de afbeelding > "
                                        + "Alt-tekst bewerken en voeg een beschrijving toe.")
                                .location(ElementLocation.slideElement(slideNum, slideTitle,
                                        pic.getShapeName(), "Afbeelding")).build());
                    }
                } else if (shape instanceof XSLFGroupShape) {
                    // Groepen zonder alt-tekst
                    XSLFGroupShape group = (XSLFGroupShape) shape;
                    if (!nl.overheid.wcag.util.AltText.present(group.getXmlObject())) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .warning().wcag("1.1.1", "A")
                                .message("Groep '" + group.getShapeName()
                                        + "' op dia " + slideNum
                                        + " mist een alternatieve tekst.")
                                .suggestion("Voeg alt-tekst toe aan de groep, of voorzie "
                                        + "individuele elementen van alt-tekst.")
                                .location(ElementLocation.slideElement(slideNum, slideTitle,
                                        group.getShapeName(), "Groep")).build());
                    }
                }
            }
        }

        return findings;
    }
}

// ============================================================================

/**
 * Checks the reading order of shapes per slide.
 */
class PptxReadingOrderCheckImpl implements PresentationCheck {
    @Override public String getCheckId() { return "PPTX_READING_ORDER_CHECK"; }
    @Override public String getDisplayName() { return "Leesvolgorde"; }

    @Override
    public List<AccessibilityFinding> check(XMLSlideShow slideShow) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        List<XSLFSlide> slides = slideShow.getSlides();

        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            int slideNum = i + 1;
            List<XSLFShape> shapes = slide.getShapes();

            if (shapes.size() > 1) {
                // Compare visual position (y, dan x) met XML-volgorde
                List<double[]> positions = new ArrayList<>();
                for (XSLFShape shape : shapes) {
                    java.awt.geom.Rectangle2D anchor = shape.getAnchor();
                    if (anchor != null) {
                        positions.add(new double[]{anchor.getY(), anchor.getX()});
                    }
                }

                // Detect if XML order deviates significantly van de visuele volgorde
                if (positions.size() > 2) {
                    boolean isOrdered = true;
                    for (int j = 1; j < positions.size(); j++) {
                        if (positions.get(j)[0] < positions.get(j - 1)[0] - 50) {
                            isOrdered = false;
                            break;
                        }
                    }

                    if (!isOrdered) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .warning().wcag("1.3.2", "A")
                                .message("De leesvolgorde op dia " + slideNum
                                        + " wijkt mogelijk af van de visuele volgorde.")
                                .suggestion("Controleer de leesvolgorde via "
                                        + "Start > Schikken > Selectievenster en pas deze aan.")
                                .location(ElementLocation.slide(slideNum, null)).build());
                    }
                }
            }
        }

        return findings;
    }
}

// ============================================================================

/**
 * Checks color contrast in text shapes.
 */
class PptxColorContrastCheckImpl implements PresentationCheck {
    @Override public String getCheckId() { return "PPTX_COLOR_CONTRAST_CHECK"; }
    @Override public String getDisplayName() { return "Kleurcontrast"; }

    @Override
    public List<AccessibilityFinding> check(XMLSlideShow slideShow) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        List<XSLFSlide> slides = slideShow.getSlides();

        slides:
        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            int slideNum = i + 1;

            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape) {
                    XSLFTextShape textShape = (XSLFTextShape) shape;
                    for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                        for (XSLFTextRun run : paragraph.getTextRuns()) {
                            java.awt.Color fontColor = null;
                            if (run.getFontColor() instanceof org.apache.poi.sl.usermodel.PaintStyle.SolidPaint solid
                                    && solid.getSolidColor() != null) {
                                fontColor = solid.getSolidColor().getColor();
                            }
                            // Detect light colors on default (white) background
                            if (fontColor != null && isLowContrast(fontColor)) {
                                findings.add(AccessibilityFinding.builder(getCheckId())
                                        .warning().wcag("1.4.3", "AA")
                                        .message("Tekst in '" + shape.getShapeName()
                                                + "' op dia " + slideNum
                                                + " heeft mogelijk onvoldoende contrast.")
                                        .suggestion("Zorg voor een contrastverhouding van "
                                                + "minimaal 4.5:1 voor normale tekst "
                                                + "en 3:1 voor grote tekst.")
                                        .location(ElementLocation.slideElement(slideNum, null,
                                                shape.getShapeName(), "Tekstvak")).build());
                                continue slides; // One finding per slide is enough
                            }
                        }
                    }
                }
            }
        }

        return findings;
    }

    private boolean isLowContrast(java.awt.Color color) {
        // Relative luminance calculation (simplified)
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen()
                + 0.114 * color.getBlue()) / 255.0;
        // Light text on assumed white background
        return luminance > 0.75;
    }
}

// ============================================================================

/**
 * Checks tables in presentations.
 */
class PptxTableCheckImpl implements PresentationCheck {
    @Override public String getCheckId() { return "PPTX_TABLE_CHECK"; }
    @Override public String getDisplayName() { return "Tabeltoegankelijkheid"; }

    @Override
    public List<AccessibilityFinding> check(XMLSlideShow slideShow) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        List<XSLFSlide> slides = slideShow.getSlides();

        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            int slideNum = i + 1;

            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTable) {
                    XSLFTable table = (XSLFTable) shape;

                    // Check complexity
                    if (table.getNumberOfRows() > 6 || table.getNumberOfColumns() > 6) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .warning().wcag("1.3.1", "A")
                                .message("Complexe tabel (" + table.getNumberOfRows()
                                        + "×" + table.getNumberOfColumns()
                                        + ") op dia " + slideNum
                                        + ". Complexe tabellen zijn moeilijk "
                                        + "toegankelijk in presentaties.")
                                .suggestion("Vereenvoudig de tabel of splits deze in "
                                        + "meerdere kleinere tabellen.")
                                .location(ElementLocation.slideElement(slideNum, null,
                                        shape.getShapeName(), "Tabel")).build());
                    }

                    // Detect empty cells
                    int emptyCells = 0;
                    for (int r = 0; r < table.getNumberOfRows(); r++) {
                        for (int c = 0; c < table.getNumberOfColumns(); c++) {
                            XSLFTableCell cell = table.getCell(r, c);
                            if (cell != null && (cell.getText() == null
                                    || cell.getText().trim().isEmpty())) {
                                emptyCells++;
                            }
                        }
                    }
                    if (emptyCells > 2) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .info().wcag("1.3.1", "A")
                                .message("Tabel op dia " + slideNum + " bevat "
                                        + emptyCells + " lege cellen.")
                                .suggestion("Vul lege cellen met een streepje (–) "
                                        + "of 'n.v.t.' zodat schermlezers de cel niet overslaan.")
                                .location(ElementLocation.slideElement(slideNum, null,
                                        shape.getShapeName(), "Tabel")).build());
                    }
                }
            }
        }

        return findings;
    }
}

// ============================================================================

/**
 * Checks hyperlink texts in presentations.
 */
class PptxLinkTextCheckImpl implements PresentationCheck {

    private static final Set<String> VAGUE_TEXTS = Set.of(
            "klik hier", "hier", "link", "lees meer", "meer info", "meer",
            "click here", "here", "read more", "more", "more info");

    @Override public String getCheckId() { return "PPTX_LINK_TEXT_CHECK"; }
    @Override public String getDisplayName() { return "Linkteksten"; }

    @Override
    public List<AccessibilityFinding> check(XMLSlideShow slideShow) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        List<XSLFSlide> slides = slideShow.getSlides();

        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            int slideNum = i + 1;

            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape) {
                    XSLFTextShape textShape = (XSLFTextShape) shape;
                    for (XSLFTextParagraph para : textShape.getTextParagraphs()) {
                        for (XSLFTextRun run : para.getTextRuns()) {
                            XSLFHyperlink link = run.getHyperlink();
                            if (link != null) {
                                String text = run.getRawText();
                                if (text == null) continue;
                                String lower = text.trim().toLowerCase();

                                if (lower.startsWith("http://") || lower.startsWith("https://")) {
                                    findings.add(AccessibilityFinding.builder(getCheckId())
                                            .warning().wcag("2.4.4", "A")
                                            .message("Hyperlink op dia " + slideNum
                                                    + " toont de URL als tekst.")
                                            .suggestion("Gebruik een beschrijvende tekst.")
                                            .location(ElementLocation.slideElement(slideNum, null,
                                                    shape.getShapeName(), "Hyperlink")).build());
                                } else if (VAGUE_TEXTS.contains(lower)) {
                                    findings.add(AccessibilityFinding.builder(getCheckId())
                                            .warning().wcag("2.4.4", "A")
                                            .message("Hyperlink '" + text + "' op dia "
                                                    + slideNum + " is niet beschrijvend.")
                                            .suggestion("Gebruik tekst die het doel beschrijft.")
                                            .location(ElementLocation.slideElement(slideNum, null,
                                                    shape.getShapeName(), "Hyperlink")).build());
                                }
                            }
                        }
                    }
                }
            }
        }

        return findings;
    }
}
