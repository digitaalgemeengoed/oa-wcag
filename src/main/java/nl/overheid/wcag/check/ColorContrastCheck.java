package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 1.4.3 (Niveau AA): Contrast (minimum).
 * Controleert tekst/achtergrond kleurcontrast.
 * Beperkte check: detecteert lichte kleuren op witte achtergrond
 * en waarschuwt bij niet-standaard kleurcombinaties.
 */
public class ColorContrastCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "COLOR_CONTRAST_CHECK"; }

    @Override
    public String getDisplayName() { return "Kleurcontrast"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            int paraIdx = context.getParagraphIndex(paragraph);
            int bodyIdx = context.getParagraphBodyElementIndex(paragraph);

            for (int r = 0; r < paragraph.getRuns().size(); r++) {
                XWPFRun run = paragraph.getRuns().get(r);
                String text = run.getText(0);
                if (text == null || text.trim().isEmpty()) continue;

                String color = run.getColor();
                if (color != null && !color.isEmpty() && !"000000".equalsIgnoreCase(color)
                        && !"auto".equalsIgnoreCase(color)) {
                    double ratio = calculateContrastRatio(color, "FFFFFF");
                    int fontSize = run.getFontSize() > 0 ? run.getFontSize() : 11;
                    boolean isLargeText = fontSize >= 18 || (fontSize >= 14 && run.isBold());
                    double requiredRatio = isLargeText ? 3.0 : 4.5;

                    if (ratio < requiredRatio) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .error()
                                .wcag("1.4.3", "AA")
                                .message(String.format(
                                        "Onvoldoende kleurcontrast: tekst #%s op witte achtergrond "
                                                + "(ratio %.1f:1, vereist %.1f:1 voor %s tekst).",
                                        color, ratio, requiredRatio,
                                        isLargeText ? "grote" : "normale"))
                                .suggestion("Gebruik een donkerdere tekstkleur om het contrast "
                                        + "te verbeteren tot minimaal " + requiredRatio + ":1.")
                                .location(ElementLocation.paragraphRun(bodyIdx, paraIdx, r, text))
                                .build());
                    }
                }
            }
        }

        return findings;
    }

    /**
     * Berekent de WCAG contrast ratio tussen twee hex-kleuren.
     */
    static double calculateContrastRatio(String hexFg, String hexBg) {
        double lumFg = relativeLuminance(hexFg);
        double lumBg = relativeLuminance(hexBg);
        double lighter = Math.max(lumFg, lumBg);
        double darker = Math.min(lumFg, lumBg);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(String hex) {
        hex = hex.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        double rLinear = linearize(r / 255.0);
        double gLinear = linearize(g / 255.0);
        double bLinear = linearize(b / 255.0);

        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear;
    }

    private static double linearize(double channel) {
        return channel <= 0.03928
                ? channel / 12.92
                : Math.pow((channel + 0.055) / 1.055, 2.4);
    }
}
