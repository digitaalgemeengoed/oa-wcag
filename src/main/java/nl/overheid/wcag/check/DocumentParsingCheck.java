package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;

import java.util.*;

/**
 * WCAG 4.1.1 (Level A): Parsing.
 * Checks for structural issues:
 * - Duplicate bookmarks
 * - Excessive empty paragraphs (whitespace via Enter)
 * - Nested tables
 */
public class DocumentParsingCheck implements AccessibilityCheck {

    private static final int MAX_CONSECUTIVE_EMPTY = 3;

    @Override
    public String getCheckId() { return "DOCUMENT_PARSING_CHECK"; }

    @Override
    public String getDisplayName() { return "Documentstructuur"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        // Controleer op opeenvolgende lege alinea's
        int consecutiveEmpty = 0;
        XWPFParagraph firstEmpty = null;

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            if (text == null || text.trim().isEmpty()) {
                if (consecutiveEmpty == 0) firstEmpty = paragraph;
                consecutiveEmpty++;
            } else {
                if (consecutiveEmpty > MAX_CONSECUTIVE_EMPTY) {
                    int paraIdx = context.getParagraphIndex(firstEmpty);
                    int bodyIdx = context.getParagraphBodyElementIndex(firstEmpty);
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .warning()
                            .wcag("4.1.1", "A")
                            .message(consecutiveEmpty + " opeenvolgende lege alinea's. "
                                    + "Gebruik witruimte via alinea-afstand in plaats van "
                                    + "lege regels.")
                            .suggestion("Verwijder de lege alinea's en stel de gewenste "
                                    + "witruimte in via Alinea > Afstand na/voor.")
                            .location(ElementLocation.paragraph(bodyIdx, paraIdx, "(lege alinea's)"))
                            .build());
                }
                consecutiveEmpty = 0;
                firstEmpty = null;
            }
        }

        // Restant na de loop
        if (consecutiveEmpty > MAX_CONSECUTIVE_EMPTY && firstEmpty != null) {
            int paraIdx = context.getParagraphIndex(firstEmpty);
            int bodyIdx = context.getParagraphBodyElementIndex(firstEmpty);
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .warning()
                    .wcag("4.1.1", "A")
                    .message(consecutiveEmpty + " opeenvolgende lege alinea's aan het einde "
                            + "van het document.")
                    .suggestion("Verwijder de lege alinea's en gebruik alinea-afstand "
                            + "voor witruimte.")
                    .location(ElementLocation.paragraph(bodyIdx, paraIdx, "(lege alinea's)"))
                    .build());
        }

        // Controleer op geneste tabellen
        for (XWPFTable table : document.getTables()) {
            int tableIdx = context.getTableIndex(table);
            int bodyIdx = context.getTableBodyElementIndex(table);

            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    if (!cell.getTables().isEmpty()) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .error()
                                .wcag("4.1.1", "A")
                                .message("Geneste tabel gedetecteerd. Nested tables "
                                        + "zijn zeer problematisch voor schermlezers.")
                                .suggestion("Splits de geneste tabel op in afzonderlijke "
                                        + "tabellen of herstructureer de gegevens.")
                                .location(ElementLocation.table(bodyIdx, tableIdx))
                                .build());
                        break; // Eén melding per tabel
                    }
                }
            }
        }

        // Controleer op dubbele bladwijzers
        Set<String> bookmarks = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            var ctp = paragraph.getCTP();
            for (var bm : ctp.getBookmarkStartArray()) {
                String name = bm.getName();
                if (name != null && !name.startsWith("_")) { // Skip interne bladwijzers
                    if (!bookmarks.add(name)) {
                        duplicates.add(name);
                    }
                }
            }
        }

        for (String dup : duplicates) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .warning()
                    .wcag("4.1.1", "A")
                    .message("Dubbele bladwijzer: '" + dup + "'.")
                    .suggestion("Hernoem of verwijder de dubbele bladwijzer.")
                    .location(ElementLocation.document())
                    .build());
        }

        return findings;
    }
}
