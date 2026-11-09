package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;

import java.util.*;

/**
 * WCAG 3.2.4 (Niveau AA): Consistente identificatie.
 * Controleert op font-wildgroei en inconsistente heading-opmaak.
 */
public class ConsistentIdentificationCheck implements AccessibilityCheck {

    private static final int MAX_FONT_FAMILIES = 5;

    @Override
    public String getCheckId() { return "CONSISTENT_IDENTIFICATION_CHECK"; }

    @Override
    public String getDisplayName() { return "Consistente identificatie"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        // Verzamel gebruikte lettertypes
        Set<String> fontFamilies = new HashSet<>();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                String font = run.getFontFamily();
                if (font != null && !font.trim().isEmpty()) {
                    fontFamilies.add(font);
                }
            }
        }

        if (fontFamilies.size() > MAX_FONT_FAMILIES) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .warning()
                    .wcag("3.2.4", "AA")
                    .message("Het document gebruikt " + fontFamilies.size()
                            + " verschillende lettertypen. Dit kan de visuele "
                            + "consistentie en herkenbaarheid verminderen.")
                    .suggestion("Beperk het aantal lettertypen tot maximaal "
                            + MAX_FONT_FAMILIES + ". Gebruik documentstijlen "
                            + "voor consistente opmaak.")
                    .location(ElementLocation.document())
                    .build());
        }

        // Controleer heading-consistentie per niveau
        Map<Integer, Set<String>> headingFonts = new HashMap<>();
        Map<Integer, Set<Integer>> headingSizes = new HashMap<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String style = paragraph.getStyle();
            int level = getHeadingLevel(style);
            if (level <= 0) continue;

            for (XWPFRun run : paragraph.getRuns()) {
                String font = run.getFontFamily();
                if (font != null) {
                    headingFonts.computeIfAbsent(level, k -> new HashSet<>()).add(font);
                }
                int size = run.getFontSize();
                if (size > 0) {
                    headingSizes.computeIfAbsent(level, k -> new HashSet<>()).add(size);
                }
            }
        }

        for (Map.Entry<Integer, Set<String>> entry : headingFonts.entrySet()) {
            if (entry.getValue().size() > 1) {
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .warning()
                        .wcag("3.2.4", "AA")
                        .message("Heading " + entry.getKey()
                                + " gebruikt inconsistente lettertypen: "
                                + String.join(", ", entry.getValue()) + ".")
                        .suggestion("Pas de heading-stijl aan zodat alle headings "
                                + "van hetzelfde niveau dezelfde opmaak hebben.")
                        .location(ElementLocation.document())
                        .build());
            }
        }

        return findings;
    }

    private int getHeadingLevel(String style) {
        if (style == null) return 0;
        if (style.matches("(?i)Heading\\s*(\\d+)")) {
            return Integer.parseInt(style.replaceAll("\\D", ""));
        }
        if (style.matches("(?i)Kop\\s*(\\d+)")) {
            return Integer.parseInt(style.replaceAll("\\D", ""));
        }
        return 0;
    }
}
