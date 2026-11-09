package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * WCAG 1.4.5 (Niveau AA): Afbeeldingen van tekst.
 * Detecteert afbeeldingen die waarschijnlijk tekst bevatten
 * (op basis van bestandsnaam en alt-tekst heuristiek).
 */
public class ImagesOfTextCheck implements AccessibilityCheck {

    private static final Pattern TEXT_IMAGE_NAME = Pattern.compile(
            ".*(screenshot|schermafbeelding|scan|tekst|text|capture|afdruk|print).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TEXT_ALT_INDICATOR = Pattern.compile(
            "^(tekst|text)\\s*:", Pattern.CASE_INSENSITIVE);

    @Override
    public String getCheckId() { return "IMAGES_OF_TEXT_CHECK"; }

    @Override
    public String getDisplayName() { return "Afbeeldingen van tekst"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            int paraIdx = context.getParagraphIndex(paragraph);
            int bodyIdx = context.getParagraphBodyElementIndex(paragraph);

            for (XWPFRun run : paragraph.getRuns()) {
                for (XWPFPicture picture : run.getEmbeddedPictures()) {
                    String imageName = null;
                    String altText = null;

                    if (picture.getCTPicture() != null
                            && picture.getCTPicture().getNvPicPr() != null
                            && picture.getCTPicture().getNvPicPr().getCNvPr() != null) {
                        CTNonVisualDrawingProps props = picture.getCTPicture().getNvPicPr().getCNvPr();
                        altText = props.getDescr();
                        imageName = props.getName();
                    }

                    boolean suspectName = imageName != null && TEXT_IMAGE_NAME.matcher(imageName).matches();
                    boolean suspectAlt = altText != null && (TEXT_ALT_INDICATOR.matcher(altText).find()
                            || altText.length() > 200);

                    if (suspectName || suspectAlt) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .warning()
                                .wcag("1.4.5", "AA")
                                .message("Mogelijke afbeelding van tekst gedetecteerd"
                                        + (imageName != null ? " ('" + imageName + "')" : "")
                                        + ". Tekst in afbeeldingen is niet schaalbaar en "
                                        + "niet toegankelijk voor schermlezers.")
                                .suggestion("Vervang de afbeelding door echte tekst met "
                                        + "dezelfde opmaak. Als de afbeelding essentieel is "
                                        + "(bijv. een logo), voeg dan een uitgebreide alt-tekst toe.")
                                .location(ElementLocation.image(bodyIdx, paraIdx,
                                        picture.getPictureData() != null
                                                ? picture.getPictureData().getFileName() : null,
                                        imageName))
                                .build());
                    }
                }
            }
        }

        return findings;
    }
}
