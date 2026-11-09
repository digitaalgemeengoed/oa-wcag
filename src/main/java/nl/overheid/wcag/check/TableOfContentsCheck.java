package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 2.4.1 (Niveau A): Blokken overslaan.
 * WCAG 2.4.5 (Niveau AA): Meerdere manieren.
 * Controleert op aanwezigheid en kwaliteit van inhoudsopgave.
 */
public class TableOfContentsCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "TABLE_OF_CONTENTS_CHECK"; }

    @Override
    public String getDisplayName() { return "Inhoudsopgave"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        // Alleen relevant voor langere documenten
        if (context.getTotalParagraphs() < 20) return findings;

        // Zoek naar TOC
        boolean hasToc = false;
        boolean tocHasHyperlinks = false;

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String style = paragraph.getStyle();
            if (style != null && (style.toLowerCase().contains("toc")
                    || style.toLowerCase().contains("inhoud"))) {
                hasToc = true;
                // Controleer op hyperlinks in TOC
                if (!paragraph.getCTP().getHyperlinkList().isEmpty()
                        || paragraph.getCTP().xmlText().contains("w:fldChar")) {
                    tocHasHyperlinks = true;
                }
            }

            // Detecteer ook via veldcodes
            String xmlText = paragraph.getCTP().xmlText();
            if (xmlText.contains("TOC") && xmlText.contains("w:instrText")) {
                hasToc = true;
            }
        }

        if (!hasToc) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .warning()
                    .wcag("2.4.5", "AA")
                    .message("Geen inhoudsopgave gevonden in dit lange document "
                            + "(" + context.getTotalParagraphs() + " alinea's).")
                    .suggestion("Voeg een inhoudsopgave toe via Verwijzingen > "
                            + "Inhoudsopgave. Dit helpt gebruikers om snel te navigeren.")
                    .location(ElementLocation.document())
                    .build());
        } else if (!tocHasHyperlinks) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .info()
                    .wcag("2.4.1", "A")
                    .message("Inhoudsopgave gevonden, maar mogelijk zonder "
                            + "werkende hyperlinks naar de secties.")
                    .suggestion("Zorg dat de inhoudsopgave klikbare links bevat "
                            + "naar de bijbehorende secties.")
                    .location(ElementLocation.document())
                    .build());
        }

        return findings;
    }
}
