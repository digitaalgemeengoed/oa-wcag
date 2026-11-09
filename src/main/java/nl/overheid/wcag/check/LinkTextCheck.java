package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * WCAG 2.4.4 (Niveau A): Linkdoel (in context).
 * Controleert of hyperlinks beschrijvende linkteksten hebben.
 */
public class LinkTextCheck implements AccessibilityCheck {

    private static final Set<String> VAGUE_LINK_TEXTS = Set.of(
            "klik hier", "hier", "meer", "lees meer", "meer info",
            "meer informatie", "link", "hier klikken", "klik",
            "click here", "here", "more", "read more", "deze link", "via deze link", "zie hier"
    );
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://|www\\.|ftp://).*", Pattern.CASE_INSENSITIVE);

    @Override
    public String getCheckId() { return "LINK_TEXT_CHECK"; }

    @Override
    public String getDisplayName() { return "Linkteksten"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            int paraIdx = context.getParagraphIndex(paragraph);
            int bodyIdx = context.getParagraphBodyElementIndex(paragraph);

            for (CTHyperlink hyperlink : paragraph.getCTP().getHyperlinkList()) {
                StringBuilder linkText = new StringBuilder();
                for (CTR r : hyperlink.getRList()) {
                    for (CTText t : r.getTList()) {
                        linkText.append(t.getStringValue());
                    }
                }

                String text = linkText.toString().trim();

                if (text.isEmpty()) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .error()
                            .wcag("2.4.4", "A")
                            .message("Hyperlink zonder zichtbare linktekst.")
                            .suggestion("Voeg een beschrijvende tekst toe aan de hyperlink.")
                            .location(ElementLocation.paragraph(bodyIdx, paraIdx,
                                    paragraph.getText()))
                            .build());
                } else if (VAGUE_LINK_TEXTS.contains(text.toLowerCase())) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .warning()
                            .wcag("2.4.4", "A")
                            .message("Vage linktekst: '" + text + "'. "
                                    + "De linktekst beschrijft het doel niet.")
                            .suggestion("Gebruik een beschrijvende linktekst die aangeeft "
                                    + "waar de link naartoe leidt, bijv. "
                                    + "'Download het jaarverslag 2024'.")
                            .location(ElementLocation.paragraph(bodyIdx, paraIdx, text))
                            .build());
                } else if (URL_PATTERN.matcher(text).matches()) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .warning()
                            .wcag("2.4.4", "A")
                            .message("URL als linktekst: '" + text + "'.")
                            .suggestion("Vervang de URL door een beschrijvende tekst. "
                                    + "Schermlezers lezen de volledige URL voor, "
                                    + "wat onprettig is voor gebruikers.")
                            .location(ElementLocation.paragraph(bodyIdx, paraIdx, text))
                            .build());
                }
            }
        }

        return findings;
    }
}
