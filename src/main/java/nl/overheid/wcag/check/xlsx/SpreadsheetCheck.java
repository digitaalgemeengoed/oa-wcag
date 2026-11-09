package nl.overheid.wcag.check.xlsx;

import nl.overheid.wcag.model.AccessibilityFinding;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;

/**
 * Interface for accessibility checks on spreadsheets (.xlsx).
 */
public interface SpreadsheetCheck {

    /** Unique identifier for this check. */
    String getCheckId();

    /** Human-readable display name. */
    String getDisplayName();

    /**
     * Execute the check on the workbook.
     *
     * @param workbook the workbook to check
     * @return list of findings (may be empty)
     */
    List<AccessibilityFinding> check(XSSFWorkbook workbook);
}
