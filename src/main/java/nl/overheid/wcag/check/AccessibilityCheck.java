package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.util.List;

/**
 * Interface for all accessibility checks.
 * Each implementation validates one or more WCAG criteria
 * and returns findings with location information.
 */
public interface AccessibilityCheck {

    /**
     * Unique identifier for this check.
     */
    String getCheckId();

    /**
     * Human-readable display name.
     */
    String getDisplayName();

    /**
     * Execute the check on the document.
     *
     * @param document   the document to check
     * @param context    context with paragraph/table indices etc.
     * @return list of findings (may be empty)
     */
    List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context);
}
