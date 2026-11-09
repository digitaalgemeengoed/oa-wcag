package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 1.4.4 (Niveau AA): Tekstgrootte aanpassen.
 * WCAG 1.4.12 (Niveau AA): Tekstafstand.
 * Controleert op te kleine fonts, exacte regelafstand, en versmalde tekenafstand.
 */
public class TextSpacingCheck implements AccessibilityCheck {

    private static final int MIN_FONT_SIZE_PT = 8;

    @Override
    public String getCheckId() { return "TEXT_SPACING_CHECK"; }

    @Override
    public String getDisplayName() { return "Tekstafstand en -grootte"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            int paraIdx = context.getParagraphIndex(paragraph);
            int bodyIdx = context.getParagraphBodyElementIndex(paragraph);
            String text = paragraph.getText();
            if (text == null || text.trim().isEmpty()) continue;

            // Check font size
            for (XWPFRun run : paragraph.getRuns()) {
                int fontSize = run.getFontSize();
                if (fontSize > 0 && fontSize < MIN_FONT_SIZE_PT) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .warning()
                            .wcag("1.4.4", "AA")
                            .message("Zeer kleine lettergrootte gedetecteerd (" + fontSize
                                    + "pt). Dit kan problemen opleveren voor slechtzienden.")
                            .suggestion("Gebruik minimaal " + MIN_FONT_SIZE_PT + "pt. "
                                    + "De standaard voor leesbare tekst is 11-12pt.")
                            .location(ElementLocation.paragraph(bodyIdx, paraIdx, text))
                            .build());
                    break; // Eén melding per alinea
                }
            }

            // Check exacte regelafstand
            CTPPr pPr = paragraph.getCTP().getPPr();
            if (pPr != null && pPr.getSpacing() != null) {
                CTSpacing spacing = pPr.getSpacing();
                if (spacing.getLineRule() == STLineSpacingRule.EXACT) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .warning()
                            .wcag("1.4.12", "AA")
                            .message("Exacte regelafstand gebruikt. Tekst wordt afgesneden "
                                    + "als de lettergrootte wordt vergroot.")
                            .suggestion("Gebruik 'Ten minste' of 'Meervoudig' als "
                                    + "regelafstandoptie in plaats van 'Exact'.")
                            .location(ElementLocation.paragraph(bodyIdx, paraIdx, text))
                            .build());
                }
            }

            // Check versmalde tekenafstand
            for (XWPFRun run : paragraph.getRuns()) {
                CTRPr rPr = run.getCTR().getRPr();
                if (rPr != null && rPr.getSpacingArray().length > 0) {
                    Object raw = rPr.getSpacingArray(0).getVal();
                    BigInteger spacingVal = raw instanceof BigInteger b ? b : null;
                    if (spacingVal != null && spacingVal.intValue() < -20) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .warning()
                                .wcag("1.4.12", "AA")
                                .message("Sterk versmalde tekenafstand gedetecteerd. "
                                        + "Dit vermindert de leesbaarheid.")
                                .suggestion("Verwijder de aangepaste tekenafstand of "
                                        + "gebruik een minder extreme waarde.")
                                .location(ElementLocation.paragraph(bodyIdx, paraIdx, text))
                                .build());
                        break;
                    }
                }
            }
        }

        return findings;
    }
}
