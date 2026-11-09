package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 1.4.1 (Niveau A): Gebruik van kleur.
 * Detecteert gekleurde tekst zonder extra opmaak (vet/cursief)
 * die erop kan wijzen dat kleur als enige onderscheider wordt gebruikt.
 */
public class UseOfColorCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "USE_OF_COLOR_CHECK"; }

    @Override
    public String getDisplayName() { return "Gebruik van kleur"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs.size() < 2) continue; // Minimaal 2 runs nodig voor kleurverschil

            boolean hasColoredRunWithoutFormatting = false;
            String coloredText = null;

            for (XWPFRun run : runs) {
                String text = run.getText(0);
                if (text == null || text.trim().isEmpty()) continue;

                String color = run.getColor();
                if (color != null && !color.isEmpty()
                        && !"000000".equalsIgnoreCase(color)
                        && !"auto".equalsIgnoreCase(color)) {
                    // Gekleurde tekst gevonden — is er extra opmaak?
                    if (!run.isBold() && !run.isItalic() && !run.getUnderline().equals(UnderlinePatterns.SINGLE)) {
                        hasColoredRunWithoutFormatting = true;
                        coloredText = text;
                        break;
                    }
                }
            }

            if (hasColoredRunWithoutFormatting) {
                int paraIdx = context.getParagraphIndex(paragraph);
                int bodyIdx = context.getParagraphBodyElementIndex(paragraph);
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .warning()
                        .wcag("1.4.1", "A")
                        .message("Gekleurde tekst zonder extra visuele aanduiding (vet/cursief/onderstreept). "
                                + "Kleur wordt mogelijk als enige onderscheider gebruikt.")
                        .suggestion("Voeg naast kleur ook een ander visueel kenmerk toe "
                                + "(bijv. vet, cursief of onderstreept) zodat de informatie "
                                + "ook zonder kleur herkenbaar is.")
                        .location(ElementLocation.paragraph(bodyIdx, paraIdx, coloredText))
                        .build());
            }
        }

        return findings;
    }
}
