package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 1.3.1 (Niveau A): Info en relaties.
 * Controleert de heading-structuur op:
 * - Overgeslagen niveaus (bijv. H1 → H3)
 * - Visuele opmaak zonder semantische heading-stijl
 * - Aanwezigheid van ten minste één heading
 */
public class HeadingStructureCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "HEADING_STRUCTURE_CHECK"; }

    @Override
    public String getDisplayName() { return "Heading-structuur"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        int lastLevel = 0;
        boolean hasHeading = false;

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String style = paragraph.getStyle();
            int level = getHeadingLevel(style);

            if (level > 0) {
                hasHeading = true;
                int paraIdx = context.getParagraphIndex(paragraph);
                int bodyIdx = context.getParagraphBodyElementIndex(paragraph);
                String text = paragraph.getText();

                // Overgeslagen niveau
                if (lastLevel > 0 && level > lastLevel + 1) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .warning()
                            .wcag("1.3.1", "A")
                            .message("Overgeslagen headingniveau: H" + lastLevel + " → H" + level + ".")
                            .suggestion("Gebruik opeenvolgende headingniveaus zonder niveaus "
                                    + "over te slaan (H1, H2, H3, etc.).")
                            .location(ElementLocation.paragraph(bodyIdx, paraIdx, text))
                            .build());
                }
                lastLevel = level;
            } else {
                // Controleer op visueel opgemaakte headings (groot/vet zonder heading-stijl)
                checkFakeHeading(paragraph, context, findings);
            }
        }

        // Geen headings gevonden
        if (!hasHeading && context.getTotalParagraphs() > 3) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .error()
                    .wcag("1.3.1", "A")
                    .message("Het document bevat geen heading-structuur.")
                    .suggestion("Gebruik heading-stijlen (Heading 1, Heading 2, etc.) "
                            + "om het document logisch te structureren.")
                    .location(ElementLocation.document())
                    .build());
        }

        return findings;
    }

    private void checkFakeHeading(XWPFParagraph paragraph, DocumentContext context,
                                   List<AccessibilityFinding> findings) {
        if (paragraph.getRuns().isEmpty()) return;
        String text = paragraph.getText();
        if (text == null || text.trim().isEmpty()) return;
        // Korte alinea's met grote/vette tekst zijn verdacht
        if (text.length() > 100) return;

        for (XWPFRun run : paragraph.getRuns()) {
            boolean isBold = run.isBold();
            int fontSize = run.getFontSize();
            if (isBold && fontSize >= 14) {
                int paraIdx = context.getParagraphIndex(paragraph);
                int bodyIdx = context.getParagraphBodyElementIndex(paragraph);
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .warning()
                        .wcag("1.3.1", "A")
                        .message("Mogelijke visuele heading zonder heading-stijl gedetecteerd "
                                + "(vet, " + fontSize + "pt).")
                        .suggestion("Als dit een heading is, gebruik dan een heading-stijl "
                                + "(bijv. Heading 1, Heading 2) in plaats van handmatige opmaak.")
                        .location(ElementLocation.paragraph(bodyIdx, paraIdx, text))
                        .build());
                break;
            }
        }
    }

    private int getHeadingLevel(String style) {
        if (style == null) return 0;
        // NL/EN heading styles
        if (style.matches("(?i)Heading\\s*(\\d+)")) {
            return Integer.parseInt(style.replaceAll("\\D", ""));
        }
        if (style.matches("(?i)Kop\\s*(\\d+)")) {
            return Integer.parseInt(style.replaceAll("\\D", ""));
        }
        return 0;
    }
}
