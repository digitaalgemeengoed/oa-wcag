package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 1.1.1 (Level A): Non-text Content.
 * Checks if all images have alternative text.
 */
public class AltTextCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "ALT_TEXT_CHECK"; }

    @Override
    public String getDisplayName() { return "Alternatieve tekst voor afbeeldingen"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            int paraIdx = context.getParagraphIndex(paragraph);
            int bodyIdx = context.getParagraphBodyElementIndex(paragraph);

            for (XWPFRun run : paragraph.getRuns()) {
                List<XWPFPicture> pictures = run.getEmbeddedPictures();
                for (XWPFPicture picture : pictures) {
                    String description = null;
                    String imageName = null;

                    if (picture.getCTPicture() != null
                            && picture.getCTPicture().getNvPicPr() != null
                            && picture.getCTPicture().getNvPicPr().getCNvPr() != null) {
                        CTNonVisualDrawingProps props = picture.getCTPicture().getNvPicPr().getCNvPr();
                        description = props.getDescr();
                        imageName = props.getName();
                    }

                    String imageId = picture.getPictureData() != null
                            ? picture.getPictureData().getFileName() : null;

                    if (description == null || description.trim().isEmpty()) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .error()
                                .wcag("1.1.1", "A")
                                .message("Afbeelding mist een alternatieve tekst (alt-tekst)."
                                        + (imageName != null ? " Bestandsnaam: '" + imageName + "'." : ""))
                                .suggestion("Voeg een beschrijvende alt-tekst toe die de inhoud of functie "
                                        + "van de afbeelding beschrijft. Als de afbeelding decoratief is, "
                                        + "markeer deze dan als decoratief.")
                                .location(ElementLocation.image(bodyIdx, paraIdx, imageId,
                                        imageName != null ? imageName : imageId))
                                .build());
                    }
                }
            }
        }
        return findings;
    }
}
