package nl.digitaalgemeengoed.wcag;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.AccessibilityReport;
import nl.overheid.wcag.model.DocumentType;
import nl.overheid.wcag.model.ElementLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a report of the checker into the shape the Office Assistant uses
 * (oa-contract: Bevinding). Severity becomes ernst, the WCAG criterion
 * becomes regel, and the location becomes a plek with a description a
 * person can read and, where there is one, the text to find it by.
 */
final class Vertaling {

    private Vertaling() {}

    /** Which WCAG 2.1 criteria the checks cover, per format (for "gecontroleerd" in the panel). */
    static final Map<DocumentType, List<String>> GECONTROLEERD = Map.of(
            DocumentType.DOCX, List.of(
                    "WCAG 2.1 1.1.1 Niet-tekstuele content", "WCAG 2.1 1.3.1 Info en relaties", "WCAG 2.1 1.3.2 Betekenisvolle volgorde",
                    "WCAG 2.1 1.3.3 Zintuiglijke eigenschappen", "WCAG 2.1 1.4.1 Gebruik van kleur", "WCAG 2.1 1.4.3 Contrast (minimum)",
                    "WCAG 2.1 1.4.4 Herschalen van tekst", "WCAG 2.1 1.4.5 Afbeeldingen van tekst", "WCAG 2.1 1.4.11 Contrast van niet-tekstuele content",
                    "WCAG 2.1 1.4.12 Tekstafstand", "WCAG 2.1 2.4.1 Blokken omzeilen", "WCAG 2.1 2.4.2 Paginatitel", "WCAG 2.1 2.4.4 Linkdoel (in context)",
                    "WCAG 2.1 2.4.5 Meerdere manieren", "WCAG 2.1 3.1.1 Taal van de pagina", "WCAG 2.1 3.2.4 Consistente identificatie",
                    "WCAG 2.1 3.3.1 Foutidentificatie", "WCAG 2.1 3.3.2 Labels of instructies", "WCAG 2.1 4.1.2 Naam, rol, waarde"),
            DocumentType.XLSX, List.of(
                    "WCAG 2.1 1.1.1 Niet-tekstuele content", "WCAG 2.1 1.3.1 Info en relaties", "WCAG 2.1 1.4.3 Contrast (minimum)",
                    "WCAG 2.1 1.4.4 Herschalen van tekst", "WCAG 2.1 2.4.2 Paginatitel", "WCAG 2.1 2.4.4 Linkdoel (in context)",
                    "WCAG 2.1 3.1.1 Taal van de pagina", "WCAG 2.1 3.2.4 Consistente identificatie"),
            DocumentType.PPTX, List.of(
                    "WCAG 2.1 1.1.1 Niet-tekstuele content", "WCAG 2.1 1.3.1 Info en relaties", "WCAG 2.1 1.3.2 Betekenisvolle volgorde",
                    "WCAG 2.1 1.4.3 Contrast (minimum)", "WCAG 2.1 2.4.2 Paginatitel", "WCAG 2.1 2.4.4 Linkdoel (in context)",
                    "WCAG 2.1 3.1.1 Taal van de pagina"));

    static Map<String, Object> antwoord(AccessibilityReport r, DocumentType type) {
        List<Map<String, Object>> bevindingen = new ArrayList<>();
        int teller = 0;
        for (AccessibilityFinding f : r.getFindings()) {
            bevindingen.add(bevinding(f, ++teller));
        }
        Map<String, Object> uit = new LinkedHashMap<>();
        uit.put("formaat", type.name().toLowerCase());
        uit.put("gecontroleerd", GECONTROLEERD.getOrDefault(type, List.of()));
        uit.put("bevindingen", bevindingen);
        return uit;
    }

    static Map<String, Object> bevinding(AccessibilityFinding f, int nummer) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("id", "wcag-" + nummer);
        b.put("module", "toegankelijkheid");
        boolean intern = "-".equals(f.getWcagCriterion());
        if (intern) {
            // A check that crashed: say so without passing on the technical details (those are in the log).
            b.put("ernst", "advies");
            b.put("regel", "—");
            b.put("melding", "Eén controle kon voor dit document niet worden uitgevoerd (" + f.getCheckId() + ").");
            b.put("controle", f.getCheckId());
            return b;
        }
        b.put("ernst", switch (f.getSeverity()) {
            case ERROR -> "fout";
            case WARNING -> "waarschuwing";
            case INFO -> "advies";
        });
        b.put("regel", "WCAG 2.1 " + f.getWcagCriterion());
        b.put("melding", f.getMessage());
        if (f.getSuggestion() != null && !f.getSuggestion().isBlank()) {
            b.put("voorstel", Map.of("beschrijving", f.getSuggestion()));
        }
        Map<String, Object> plek = plek(f.getLocation());
        if (plek != null) b.put("plek", plek);
        b.put("controle", f.getCheckId());
        return b;
    }

    /** A place a person can read, plus what an adapter needs to find it ("fragment": text to search for). */
    static Map<String, Object> plek(ElementLocation l) {
        if (l == null) return null;
        Map<String, Object> p = new LinkedHashMap<>();
        String fragment = kort(l.getTextSnippet(), 60);
        String beschrijving = switch (l.getType()) {
            case DOCUMENT -> "Het document (eigenschappen)";
            case PARAGRAPH -> fragment != null ? "Alinea \"" + kort(fragment, 40) + "\"" : "Alinea " + nummer(l.getParagraphIndex());
            case TABLE -> "Tabel " + nummer(l.getTableIndex());
            case TABLE_CELL -> "Tabel " + nummer(l.getTableIndex()) + ", rij " + nummer(l.getRowIndex()) + ", cel " + nummer(l.getCellIndex());
            case IMAGE -> l.getImageName() != null ? "Afbeelding (" + l.getImageName() + ")" : "Afbeelding";
            case HEADER -> "Koptekst";
            case FOOTER -> "Voettekst";
            case STYLE -> "Opmaakprofiel \"" + (l.getStyleName() != null ? l.getStyleName() : l.getStyleId()) + "\"";
            case SHEET -> "Blad \"" + l.getSheetName() + "\"";
            case SPREADSHEET_CELL -> "Blad \"" + l.getSheetName() + "\", cel " + l.getCellReference();
            case CHART -> "Grafiek \"" + l.getChartName() + "\" op blad \"" + l.getSheetName() + "\"";
            case SLIDE -> "Dia " + l.getSlideNumber() + (l.getSlideTitle() != null ? " \"" + kort(l.getSlideTitle(), 40) + "\"" : "");
            case SLIDE_ELEMENT -> "Dia " + l.getSlideNumber() + " · " + (l.getShapeType() != null ? l.getShapeType().toLowerCase() + " " : "")
                    + (l.getShapeName() != null ? "\"" + l.getShapeName() + "\"" : "");
        };
        p.put("beschrijving", beschrijving.trim());
        if (fragment != null && !fragment.isBlank()) p.put("fragment", fragment);
        // The raw location, for adapters that can go there directly.
        Map<String, Object> waar = new LinkedHashMap<>();
        waar.put("soort", l.getType().name().toLowerCase());
        zet(waar, "alinea", l.getParagraphIndex());
        zet(waar, "tabel", l.getTableIndex());
        zet(waar, "rij", l.getRowIndex());
        zet(waar, "cel", l.getCellIndex());
        zet(waar, "blad", l.getSheetName());
        zet(waar, "celverwijzing", l.getCellReference());
        zet(waar, "dia", l.getSlideNumber());
        zet(waar, "vorm", l.getShapeName());
        p.put("waar", waar);
        return p;
    }

    private static void zet(Map<String, Object> m, String k, Object v) {
        if (v != null) m.put(k, v);
    }

    private static String nummer(Integer i) {
        return i == null ? "?" : String.valueOf(i + 1);
    }

    private static String kort(String s, int n) {
        if (s == null) return null;
        String t = s.replaceAll("\\s+", " ").trim();
        if (t.isEmpty()) return null;
        return t.length() > n ? t.substring(0, n - 1) + "…" : t;
    }
}
