package nl.overheid.wcag.check.pptx;

import nl.overheid.wcag.model.AccessibilityFinding;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.util.List;

/**
 * Interface for accessibility checks on presentations (.pptx).
 */
public interface PresentationCheck {

    /** Unique identifier for this check. */
    String getCheckId();

    /** Human-readable display name. */
    String getDisplayName();

    /**
     * Execute the check on the presentation.
     *
     * @param slideShow the presentation to check
     * @return list of findings (may be empty)
     */
    List<AccessibilityFinding> check(XMLSlideShow slideShow);
}
