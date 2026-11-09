package nl.overheid.wcag.model;

/**
 * Metadata profiles for document checking.
 *
 * <p>Determines which metadata fields are checked:
 * <ul>
 *   <li><b>WCAG</b> — Alleen titel en taal (WCAG 2.4.2 en 3.1.1)</li>
 *   <li><b>MDTO_BASIS</b> — WCAG + auteur, onderwerp, trefwoorden</li>
 *   <li><b>MDTO_UITGEBREID</b> — Alle velden inclusief categorie, datums, status</li>
 * </ul>
 *
 * @see <a href="https://www.nationaalarchief.nl/archiveren/mdto">MDTO standaard</a>
 */
public enum MetadataProfile {

    /**
     * Only WCAG-required fields: title and language.
     */
    WCAG("WCAG 2.1 AA", "Alleen titel en taal"),

    /**
     * WCAG + basic fields for information management:
     * author, subject, keywords.
     */
    MDTO_BASIS("MDTO Basis", "WCAG + auteur, onderwerp, trefwoorden"),

    /**
     * Full profile including category, comments,
     * creation date, modification date, status.
     */
    MDTO_UITGEBREID("MDTO Uitgebreid", "Alle metadata-velden");

    private final String displayName;
    private final String description;

    MetadataProfile(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    /**
     * Parse a profile from a string (case-insensitive).
     * Falls back to WCAG if not recognized.
     */
    public static MetadataProfile fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return WCAG;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return WCAG;
        }
    }
}
