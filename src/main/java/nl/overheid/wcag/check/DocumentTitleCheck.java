package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 2.4.2 (Level A): Page Titled.
 * Checks if the document has a title in its properties.
 */
public class DocumentTitleCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "DOCUMENT_TITLE_CHECK"; }

    @Override
    public String getDisplayName() { return "Documenttitel"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        String title = null;
        try {
            if (document.getProperties() != null
                    && document.getProperties().getCoreProperties() != null) {
                title = document.getProperties().getCoreProperties().getTitle();
            }
        } catch (Exception e) {
            // Properties niet beschikbaar
        }

        if (title == null || title.trim().isEmpty()) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .error()
                    .wcag("2.4.2", "A")
                    .message("Het document heeft geen titel in de documenteigenschappen.")
                    .suggestion("Stel een beschrijvende titel in via Bestand > Eigenschappen > Titel, "
                            + "of via Bestand > Info > Eigenschappen.")
                    .location(ElementLocation.document())
                    .build());
        }

        return findings;
    }
}
