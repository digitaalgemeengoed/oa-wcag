package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.util.ArrayList;
import java.util.List;

/**
 * WCAG 3.3.1 (Niveau A): Foutidentificatie.
 * WCAG 3.3.2 (Niveau A): Labels of instructies.
 * WCAG 4.1.2 (Niveau A): Naam, rol, waarde.
 * Controleert formuliervelden op labels en hulptekst.
 */
public class FormFieldCheck implements AccessibilityCheck {

    @Override
    public String getCheckId() { return "FORM_FIELD_CHECK"; }

    @Override
    public String getDisplayName() { return "Formuliervelden"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            int paraIdx = context.getParagraphIndex(paragraph);
            int bodyIdx = context.getParagraphBodyElementIndex(paragraph);
            CTP ctp = paragraph.getCTP();

            // Controleer legacy form fields (FFData)
            for (CTR ctr : ctp.getRArray()) {
                if (ctr.sizeOfFldCharArray() > 0) {
                    for (CTFldChar fldChar : ctr.getFldCharArray()) {
                        if (fldChar.getFfData() != null) {
                            CTFFData ffData = fldChar.getFfData();
                            boolean hasName = ffData.sizeOfNameArray() > 0
                                    && ffData.getNameArray(0).getVal() != null
                                    && !ffData.getNameArray(0).getVal().trim().isEmpty();
                            boolean hasHelp = ffData.sizeOfHelpTextArray() > 0;
                            boolean hasStatus = ffData.sizeOfStatusTextArray() > 0;

                            if (!hasName) {
                                findings.add(AccessibilityFinding.builder(getCheckId())
                                        .error()
                                        .wcag("4.1.2", "A")
                                        .message("Formulierveld zonder naam/label.")
                                        .suggestion("Geef het formulierveld een beschrijvend "
                                                + "label via de veldeigenschappen.")
                                        .location(ElementLocation.paragraph(bodyIdx, paraIdx,
                                                paragraph.getText()))
                                        .build());
                            }
                            if (!hasHelp && !hasStatus) {
                                findings.add(AccessibilityFinding.builder(getCheckId())
                                        .warning()
                                        .wcag("3.3.2", "A")
                                        .message("Formulierveld zonder hulptekst of "
                                                + "statusbalktekst.")
                                        .suggestion("Voeg hulptekst toe aan het veld "
                                                + "zodat gebruikers weten wat er "
                                                + "moet worden ingevuld.")
                                        .location(ElementLocation.paragraph(bodyIdx, paraIdx,
                                                paragraph.getText()))
                                        .build());
                            }
                        }
                    }
                }
            }

            // Controleer Content Controls (SDT)
            // Block-level content controls (w:sdt directly in the body). POI has no list of
            // all content controls, so read them from the XML of the body.
            for (CTSdtBlock block : document.getDocument().getBody().getSdtList()) {
                CTSdtPr sdtPr = block.getSdtPr();
                if (sdtPr != null) {
                    boolean hasTitle = false;
                    try {
                        // The alias is the title shown to the user
                        if (sdtPr.getAlias() != null
                                && sdtPr.getAlias().getVal() != null
                                && !sdtPr.getAlias().getVal().trim().isEmpty()) {
                            hasTitle = true;
                        }
                    } catch (Exception e) {
                        // SDT properties not fully available
                    }

                    if (!hasTitle) {
                        findings.add(AccessibilityFinding.builder(getCheckId())
                                .warning()
                                .wcag("4.1.2", "A")
                                .message("Inhoudsbesturingselement (Content Control) "
                                        + "zonder titel/label.")
                                .suggestion("Stel een titel in voor het "
                                        + "inhoudsbesturingselement via de eigenschappen.")
                                .location(ElementLocation.document())
                                .build());
                    }
                }
            }
        }

        return findings;
    }
}
