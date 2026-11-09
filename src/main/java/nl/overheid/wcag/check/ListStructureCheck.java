package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * WCAG 1.3.1 (Niveau A): Info en relaties.
 * Controleert of lijsten correct zijn opgemaakt met lijststijlen
 * in plaats van handmatige opsommingstekens.
 */
public class ListStructureCheck implements AccessibilityCheck {

    private static final Pattern FAKE_BULLET_PATTERN = Pattern.compile(
            "^\\s*[\\-\\*•●◦▪►→\\>]\\s+.*");
    private static final Pattern FAKE_NUMBER_PATTERN = Pattern.compile(
            "^\\s*\\d{1,3}[\\)\\.\\:]\\s+.*");

    @Override
    public String getCheckId() { return "LIST_STRUCTURE_CHECK"; }

    @Override
    public String getDisplayName() { return "Lijststructuur"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        int consecutiveFakeBullets = 0;
        XWPFParagraph firstFakeBullet = null;

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            if (text == null || text.trim().isEmpty()) {
                if (consecutiveFakeBullets >= 2) {
                    reportFakeList(firstFakeBullet, consecutiveFakeBullets, context, findings);
                }
                consecutiveFakeBullets = 0;
                firstFakeBullet = null;
                continue;
            }

            boolean isRealList = paragraph.getNumID() != null;
            boolean isFakeBullet = !isRealList
                    && (FAKE_BULLET_PATTERN.matcher(text).matches()
                    || FAKE_NUMBER_PATTERN.matcher(text).matches());

            if (isFakeBullet) {
                if (consecutiveFakeBullets == 0) {
                    firstFakeBullet = paragraph;
                }
                consecutiveFakeBullets++;
            } else {
                if (consecutiveFakeBullets >= 2) {
                    reportFakeList(firstFakeBullet, consecutiveFakeBullets, context, findings);
                }
                consecutiveFakeBullets = 0;
                firstFakeBullet = null;
            }
        }

        // Restant na de loop
        if (consecutiveFakeBullets >= 2) {
            reportFakeList(firstFakeBullet, consecutiveFakeBullets, context, findings);
        }

        return findings;
    }

    private void reportFakeList(XWPFParagraph firstPara, int count,
                                 DocumentContext context, List<AccessibilityFinding> findings) {
        if (firstPara == null) return;
        int paraIdx = context.getParagraphIndex(firstPara);
        int bodyIdx = context.getParagraphBodyElementIndex(firstPara);

        findings.add(AccessibilityFinding.builder(getCheckId())
                .warning()
                .wcag("1.3.1", "A")
                .message("Handmatige opsomming gedetecteerd (" + count
                        + " items). Gebruik een lijststijl voor correcte semantiek.")
                .suggestion("Selecteer de items en pas een opsommingslijst- of "
                        + "genummerde-lijststijl toe via de opmaakwerkbalk.")
                .location(ElementLocation.paragraph(bodyIdx, paraIdx, firstPara.getText()))
                .build());
    }
}
