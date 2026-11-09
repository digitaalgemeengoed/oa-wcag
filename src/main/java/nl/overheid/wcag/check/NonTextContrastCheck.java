package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 1.4.11 (Niveau AA): Contrast van niet-tekstuele content.
 * Controleert tabelranden op zichtbaarheid en contrast.
 */
public class NonTextContrastCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "NON_TEXT_CONTRAST_CHECK"; }

    @Override
    public String getDisplayName() { return "Niet-tekstueel contrast"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (XWPFTable table : document.getTables()) {
            int tableIdx = context.getTableIndex(table);
            int bodyIdx = context.getTableBodyElementIndex(table);

            // Controleer of de tabel randen heeft
            CTTblPr tblPr = table.getCTTbl().getTblPr();
            if (tblPr != null && tblPr.getTblBorders() != null) {
                CTTblBorders borders = tblPr.getTblBorders();

                boolean hasVisibleBorders = hasVisibleBorder(borders.getTop())
                        || hasVisibleBorder(borders.getBottom())
                        || hasVisibleBorder(borders.getLeft())
                        || hasVisibleBorder(borders.getRight())
                        || hasVisibleBorder(borders.getInsideH())
                        || hasVisibleBorder(borders.getInsideV());

                if (!hasVisibleBorders && table.getNumberOfRows() > 1) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .info()
                            .wcag("1.4.11", "AA")
                            .message("Tabel zonder zichtbare randen. Dit kan het moeilijk "
                                    + "maken om de tabelstructuur visueel te herkennen.")
                            .suggestion("Voeg zichtbare randen toe of gebruik voldoende "
                                    + "witruimte en achtergrondkleurverschillen om de "
                                    + "tabelstructuur duidelijk te maken.")
                            .location(ElementLocation.table(bodyIdx, tableIdx))
                            .build());
                }

                // Controleer op lichte randkleuren
                checkBorderContrast(borders.getTop(), "bovenkant", bodyIdx, tableIdx, findings);
                checkBorderContrast(borders.getInsideH(), "horizontale binnenrand",
                        bodyIdx, tableIdx, findings);
                checkBorderContrast(borders.getInsideV(), "verticale binnenrand",
                        bodyIdx, tableIdx, findings);
            }
        }

        return findings;
    }

    private boolean hasVisibleBorder(CTBorder border) {
        if (border == null) return false;
        STBorder.Enum val = border.getVal();
        return val != null && val != STBorder.NONE && val != STBorder.NIL;
    }

    private void checkBorderContrast(CTBorder border, String borderName,
                                      int bodyIdx, int tableIdx,
                                      List<AccessibilityFinding> findings) {
        if (border == null || !hasVisibleBorder(border)) return;

        Object colorObj = border.getColor();
        if (colorObj != null) {
            String color = colorObj.toString();
            if (!"auto".equalsIgnoreCase(color) && !"000000".equalsIgnoreCase(color)) {
                try {
                    double ratio = ColorContrastCheck.calculateContrastRatio(color, "FFFFFF");
                    if (ratio < 3.0) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .warning()
                                .wcag("1.4.11", "AA")
                                .message(String.format(
                                        "Onvoldoende contrast voor tabel-%s "
                                                + "(kleur #%s, ratio %.1f:1, vereist 3:1).",
                                        borderName, color, ratio))
                                .suggestion("Gebruik een donkerdere randkleur met een "
                                        + "contrastratio van minimaal 3:1.")
                                .location(ElementLocation.table(bodyIdx, tableIdx))
                                .build());
                    }
                } catch (Exception e) {
                    // Kleur kan niet worden geparsed
                }
            }
        }
    }
}
