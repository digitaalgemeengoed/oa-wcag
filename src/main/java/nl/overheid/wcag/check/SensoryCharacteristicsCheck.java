package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import org.apache.poi.xwpf.usermodel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * WCAG 1.3.3 (Niveau A): Zintuiglijke eigenschappen.
 * Detecteert tekst die verwijst naar visuele/auditieve kenmerken
 * als enige manier om informatie over te brengen.
 */
public class SensoryCharacteristicsCheck implements AccessibilityCheck {

    // NL patronen
    private static final Pattern VISUAL_REF_NL = Pattern.compile(
            "\\b(klik\\s+(op\\s+)?(de\\s+)?(links|rechts|bovenaan|onderaan|hiernaast|hierboven|hieronder)"
                    + "|de\\s+(rode|groene|blauwe|gele|oranje|gekleurde)\\s+(knop|tekst|balk|kader|blok)"
                    + "|aangegeven\\s+in\\s+(rood|groen|blauw|geel|oranje|het\\s+\\w+)"
                    + "|zie\\s+(de\\s+)?(afbeelding|figuur|tabel)\\s+(links|rechts|hierboven|hieronder|hiernaast)"
                    + "|in\\s+de\\s+(linker|rechter)\\s*kolom"
                    + "|het\\s+(ronde|vierkante|driehoekige)\\s+icoon)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // EN patronen
    private static final Pattern VISUAL_REF_EN = Pattern.compile(
            "\\b(click\\s+(the\\s+)?(left|right|above|below)"
                    + "|the\\s+(red|green|blue|yellow|orange|colored)\\s+(button|text|bar|box|block)"
                    + "|indicated\\s+in\\s+(red|green|blue|yellow)"
                    + "|see\\s+(the\\s+)?(image|figure|table)\\s+(on\\s+the\\s+)?(left|right|above|below)"
                    + "|in\\s+the\\s+(left|right)\\s*column"
                    + "|the\\s+(round|square|triangular)\\s+icon)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String getCheckId() { return "SENSORY_CHARACTERISTICS_CHECK"; }

    @Override
    public String getDisplayName() { return "Zintuiglijke kenmerken"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            if (text == null || text.trim().isEmpty()) continue;

            boolean matchNl = VISUAL_REF_NL.matcher(text).find();
            boolean matchEn = VISUAL_REF_EN.matcher(text).find();

            if (matchNl || matchEn) {
                int paraIdx = context.getParagraphIndex(paragraph);
                int bodyIdx = context.getParagraphBodyElementIndex(paragraph);
                findings.add(AccessibilityFinding.builder(getCheckId())
                        .warning()
                        .wcag("1.3.3", "A")
                        .message("Mogelijke verwijzing naar zintuiglijke kenmerken "
                                + "(kleur, vorm of positie) als enige informatiedrager.")
                        .suggestion("Zorg dat informatie niet alleen via kleur, vorm of "
                                + "positie wordt overgebracht. Voeg een tekst-alternatief toe.")
                        .location(ElementLocation.paragraph(bodyIdx, paraIdx, text))
                        .build());
            }
        }

        return findings;
    }
}
