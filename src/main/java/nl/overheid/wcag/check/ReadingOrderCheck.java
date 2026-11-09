package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColumns;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 1.3.2 (Niveau A): Betekenisvolle volgorde.
 * Detecteert elementen die de leesvolgorde kunnen verstoren:
 * tekstvakken, zwevende objecten, meerkolomsindelingen.
 */
public class ReadingOrderCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "READING_ORDER_CHECK"; }

    @Override
    public String getDisplayName() { return "Leesvolgorde"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        // Controleer op tekstvakken en zwevende objecten
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            int paraIdx = context.getParagraphIndex(paragraph);
            int bodyIdx = context.getParagraphBodyElementIndex(paragraph);

            // Detecteer tekstvakken via w:pict of mc:AlternateContent
            var ctp = paragraph.getCTP();
            String xmlText = ctp.xmlText();
            if (xmlText.contains("wsp:txbx") || xmlText.contains("w:txbxContent")
                    || xmlText.contains("mc:AlternateContent")) {
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .warning()
                        .wcag("1.3.2", "A")
                        .message("Tekstvak of zwevend object gedetecteerd. "
                                + "Dit kan de leesvolgorde voor schermlezers verstoren.")
                        .suggestion("Overweeg de tekst direct in het document te plaatsen "
                                + "in plaats van in een tekstvak. Als een tekstvak nodig is, "
                                + "zorg dan dat de alt-tekst en leesvolgorde correct zijn.")
                        .location(ElementLocation.paragraph(bodyIdx, paraIdx, paragraph.getText()))
                        .build());
            }
        }

        // Controleer op meerkolomsindelingen
        try {
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            if (sectPr != null && sectPr.getCols() != null) {
                CTColumns cols = sectPr.getCols();
                int numCols = cols.getNum() != null ? cols.getNum().intValue() : 1;
                if (numCols > 1) {
                    findings.add(AccessibilityFinding.builder(getCheckId())
                            .info()
                            .wcag("1.3.2", "A")
                            .message("Document gebruikt een " + numCols + "-kolomsindeling. "
                                    + "Controleer of de leesvolgorde correct is.")
                            .suggestion("Meerkolomsindelingen via secties zijn doorgaans "
                                    + "goed interpreteerbaar. Vermijd het simuleren van "
                                    + "kolommen met tabellen of tabs.")
                            .location(ElementLocation.document())
                            .build());
                }
            }
        } catch (Exception e) {
            // SectPr niet beschikbaar
        }

        return findings;
    }
}
