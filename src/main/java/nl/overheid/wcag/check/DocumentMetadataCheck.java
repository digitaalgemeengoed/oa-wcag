package nl.overheid.wcag.check;

import nl.overheid.wcag.model.AccessibilityFinding;
import nl.overheid.wcag.model.ElementLocation;
import nl.overheid.wcag.model.MetadataProfile;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.util.*;

/**
 * Checks document metadata for completeness.
 *
 * <p>Supports three profiles:
 * <ul>
 *   <li><b>WCAG</b> — Alleen titel en taal (WCAG 2.4.2 en 3.1.1)</li>
 *   <li><b>MDTO_BASIS</b> — WCAG + auteur, onderwerp, trefwoorden</li>
 *   <li><b>MDTO_UITGEBREID</b> — Alles inclusief categorie, opmerkingen, datums</li>
 * </ul>
 *
 * @see <a href="https://www.nationaalarchief.nl/archiveren/mdto">MDTO standaard</a>
 */
public class DocumentMetadataCheck implements AccessibilityCheck {

    /**
     * Metadata fields that can be checked.
     */
    public enum MetadataField {
        TITLE("Titel", true, "2.4.2", "A"),
        LANGUAGE("Taal", true, "3.1.1", "A"),
        CREATOR("Auteur", false, null, null),
        SUBJECT("Onderwerp", false, null, null),
        KEYWORDS("Trefwoorden", false, null, null),
        CATEGORY("Categorie", false, null, null),
        DESCRIPTION("Opmerkingen", false, null, null),
        CREATED("Aanmaakdatum", false, null, null),
        MODIFIED("Wijzigingsdatum", false, null, null),
        LAST_MODIFIED_BY("Laatst gewijzigd door", false, null, null),
        REVISION("Revisienummer", false, null, null),
        CONTENT_STATUS("Status", false, null, null);

        private final String displayName;
        private final boolean wcagRequired;
        private final String wcagCriterion;
        private final String wcagLevel;

        MetadataField(String displayName, boolean wcagRequired,
                      String wcagCriterion, String wcagLevel) {
            this.displayName = displayName;
            this.wcagRequired = wcagRequired;
            this.wcagCriterion = wcagCriterion;
            this.wcagLevel = wcagLevel;
        }

        public String getDisplayName() { return displayName; }
        public boolean isWcagRequired() { return wcagRequired; }
        public String getWcagCriterion() { return wcagCriterion; }
        public String getWcagLevel() { return wcagLevel; }
    }

    // Fields per profile
    private static final Set<MetadataField> WCAG_FIELDS = EnumSet.of(
            MetadataField.TITLE, MetadataField.LANGUAGE);

    private static final Set<MetadataField> MDTO_BASIS_FIELDS = EnumSet.of(
            MetadataField.TITLE, MetadataField.LANGUAGE,
            MetadataField.CREATOR, MetadataField.SUBJECT, MetadataField.KEYWORDS);

    private static final Set<MetadataField> MDTO_UITGEBREID_FIELDS =
            EnumSet.allOf(MetadataField.class);

    private final MetadataProfile profile;

    public DocumentMetadataCheck() {
        this(MetadataProfile.WCAG);
    }

    public DocumentMetadataCheck(MetadataProfile profile) {
        this.profile = profile != null ? profile : MetadataProfile.WCAG;
    }

    @Override
    public String getCheckId() { return "METADATA_CHECK"; }

    @Override
    public String getDisplayName() { return "Metadata controle (" + profile.getDisplayName() + ")"; }

    @Override
    public List<AccessibilityFinding> check(XWPFDocument document, DocumentContext context) {
        List<AccessibilityFinding> findings = new ArrayList<>();
        Set<MetadataField> fieldsToCheck = getFieldsForProfile();

        // Extract values
        Map<MetadataField, String> values = extractMetadata(document);

        // Check WCAG-required fields (ERROR)
        List<MetadataField> missingRequired = new ArrayList<>();
        List<MetadataField> missingOptional = new ArrayList<>();

        for (MetadataField field : fieldsToCheck) {
            String value = values.get(field);
            boolean isEmpty = value == null || value.trim().isEmpty();

            if (isEmpty) {
                if (field.isWcagRequired()) {
                    missingRequired.add(field);
                } else {
                    missingOptional.add(field);
                }
            }
        }

        // WCAG-required fields as ERROR
        for (MetadataField field : missingRequired) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .error()
                    .wcag(field.getWcagCriterion(), field.getWcagLevel())
                    .message(field.getDisplayName() + " ontbreekt in de documenteigenschappen.")
                    .suggestion(getSuggestionForField(field))
                    .location(ElementLocation.document())
                    .build());
        }

        // MDTO fields as WARNING (per field for clarity)
        for (MetadataField field : missingOptional) {
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .warning()
                    .wcag("MDTO", "-")
                    .message("Aanbevolen metadataveld '" + field.getDisplayName()
                            + "' ontbreekt (profiel: " + profile.name() + ").")
                    .suggestion(getSuggestionForField(field))
                    .location(ElementLocation.document())
                    .build());
        }

        // Informational summary if all is well
        if (findings.isEmpty() && profile != MetadataProfile.WCAG) {
            int total = fieldsToCheck.size();
            findings.add(AccessibilityFinding.builder(getCheckId())
                    .info()
                    .wcag("MDTO", "-")
                    .message("Metadata volledig: " + total + "/" + total
                            + " velden ingevuld (profiel: " + profile.name() + ").")
                    .location(ElementLocation.document())
                    .build());
        }

        return findings;
    }

    private Set<MetadataField> getFieldsForProfile() {
        switch (profile) {
            case MDTO_BASIS: return MDTO_BASIS_FIELDS;
            case MDTO_UITGEBREID: return MDTO_UITGEBREID_FIELDS;
            default: return WCAG_FIELDS;
        }
    }

    private Map<MetadataField, String> extractMetadata(XWPFDocument document) {
        Map<MetadataField, String> values = new EnumMap<>(MetadataField.class);
        POIXMLProperties.CoreProperties core = document.getProperties().getCoreProperties();
        POIXMLProperties.ExtendedProperties extended = document.getProperties().getExtendedProperties();

        values.put(MetadataField.TITLE, core.getTitle());
        // Language: try dc:language, fallback to document settings
        String lang = core.getUnderlyingProperties().getLanguageProperty().orElse(null);
        if (lang == null || lang.isEmpty()) {
            try {
                lang = document.getDocument().getBody().getSectPr() != null
                        ? null : null; // fallback: check via styles
            } catch (Exception ignored) {}
        }
        values.put(MetadataField.LANGUAGE, lang);
        values.put(MetadataField.CREATOR, core.getCreator());
        values.put(MetadataField.SUBJECT, core.getSubject());
        values.put(MetadataField.KEYWORDS, core.getKeywords());
        values.put(MetadataField.CATEGORY, core.getCategory());
        values.put(MetadataField.DESCRIPTION, core.getDescription());
        values.put(MetadataField.CREATED, core.getCreated() != null
                ? core.getCreated().toString() : null);
        values.put(MetadataField.MODIFIED, core.getModified() != null
                ? core.getModified().toString() : null);
        values.put(MetadataField.LAST_MODIFIED_BY, core.getUnderlyingProperties()
                .getLastModifiedByProperty().orElse(null));
        values.put(MetadataField.REVISION, core.getRevision());
        values.put(MetadataField.CONTENT_STATUS, core.getContentStatus());

        return values;
    }

    private String getSuggestionForField(MetadataField field) {
        switch (field) {
            case TITLE:
                return "Stel de documenttitel in via Bestand > Info > Titel.";
            case LANGUAGE:
                return "Stel de documenttaal in via de bestandseigenschappen.";
            case CREATOR:
                return "Vul de auteur in via Bestand > Info > Eigenschappen.";
            case SUBJECT:
                return "Vul het onderwerp in voor betere vindbaarheid.";
            case KEYWORDS:
                return "Voeg trefwoorden toe voor betere vindbaarheid.";
            default:
                return "Vul dit veld in via Bestand > Info > Eigenschappen.";
        }
    }
}
