package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 1.3.1 (Niveau A): Info en relaties.
 * Controleert of tabellen een gemarkeerde koprij hebben.
 */
public class TableHeaderCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "TABLE_HEADER_CHECK"; }

    @Override
    public String getDisplayName() { return "Tabelkoppen"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (XWPFTable table : document.getTables()) {
            int tableIdx = context.getTableIndex(table);
            int bodyIdx = context.getTableBodyElementIndex(table);

            if (table.getNumberOfRows() < 2) continue; // Eenrijige tabel, geen header nodig

            XWPFTableRow firstRow = table.getRow(0);
            boolean hasHeaderRow = false;

            // Controleer of de eerste rij als header is gemarkeerd
            CTRow ctRow = firstRow.getCtRow();
            if (ctRow.getTrPr() != null) {
                CTTrPr trPr = ctRow.getTrPr();
                if (trPr.sizeOfTblHeaderArray() > 0) {
                    hasHeaderRow = true;
                }
            }

            if (!hasHeaderRow) {
                String firstCellText = "";
                if (!firstRow.getTableCells().isEmpty()) {
                    firstCellText = firstRow.getTableCells().get(0).getText();
                }
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .error()
                        .wcag("1.3.1", "A")
                        .message("Tabel mist een gemarkeerde koprij.")
                        .suggestion("Markeer de eerste rij als kopriij: selecteer de rij, "
                                + "ga naar Tabeleigenschappen > Rij en vink "
                                + "'Rij herhalen als koptekst' aan.")
                        .location(ElementLocation.tableCell(bodyIdx, tableIdx, 0, 0, firstCellText))
                        .build());
            }
        }

        return findings;
    }
}
