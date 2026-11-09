package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 3.1.1 (Level A): Language of Page.
 * Checks if the document language is set.
 */
public class DocumentLanguageCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "DOCUMENT_LANGUAGE_CHECK"; }

    @Override
    public String getDisplayName() { return "Documenttaal"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        // Probeer de taal uit de document properties te halen
        String language = null;
        try {
            if (document.getProperties() != null
                    && document.getProperties().getCoreProperties() != null) {
                language = document.getProperties().getCoreProperties()
                        .getUnderlyingProperties().getLanguageProperty().orElse(null);
            }
        } catch (Exception e) {
            // Properties niet beschikbaar
        }

        // Fallback 1: the default language in styles.xml (w:docDefaults/w:rPrDefault).
        // This is where Word and LibreOffice normally store the document language.
        if (language == null || language.trim().isEmpty()) {
            try {
                var styles = document.getStyle();
                if (styles != null && styles.getDocDefaults() != null
                        && styles.getDocDefaults().getRPrDefault() != null
                        && styles.getDocDefaults().getRPrDefault().getRPr() != null) {
                    var langs = styles.getDocDefaults().getRPrDefault().getRPr().getLangArray();
                    if (langs.length > 0 && langs[0].getVal() != null) {
                        language = langs[0].getVal();
                    }
                }
            } catch (Exception e) {
                // Styles not available
            }
        }

        // Fallback 2: the language of the first paragraph
        if (language == null || language.trim().isEmpty()) {
            try {
                CTDocument1 ctDoc = document.getDocument();
                if (ctDoc.getBody() != null && ctDoc.getBody().sizeOfPArray() > 0) {
                    var firstPara = ctDoc.getBody().getPArray(0);
                    if (firstPara.getPPr() != null && firstPara.getPPr().getRPr() != null
                            && firstPara.getPPr().getRPr().getLangArray().length > 0) {
                        language = firstPara.getPPr().getRPr().getLangArray(0).getVal();
                    }
                }
            } catch (Exception e) {
                // Fallback not available
            }
        }

        if (language == null || language.trim().isEmpty()) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .error()
                    .wcag("3.1.1", "A")
                    .message("De documenttaal is niet ingesteld.")
                    .suggestion("Stel de documenttaal in via Bestand > Opties > Taal, "
                            + "of stel de taal in bij de alinea-eigenschappen.")
                    .location(ElementLocation.document())
                    .build());
        }

        return findings;
    }
}
