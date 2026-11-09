package nl.overheid.wcag.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Complete accessibility report for a document.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessibilityReport {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private String documentId;
    private String sourceApplication;
    private String callbackUrl;
    private String timestamp;
    private DocumentType documentType;
    private MetadataProfile metadataProfile;
    private Summary summary;
    private List<AccessibilityFinding> findings;

    public AccessibilityReport() {
        this.findings = new ArrayList<>();
        this.timestamp = Instant.now().toString();
    }

    public void addFinding(AccessibilityFinding finding) {
        findings.add(finding);
    }

    public void addAllFindings(List<AccessibilityFinding> findings) {
        this.findings.addAll(findings);
    }

    public void buildSummary() {
        long errors = findings.stream()
                .filter(f -> f.getSeverity() == AccessibilityFinding.Severity.ERROR).count();
        long warnings = findings.stream()
                .filter(f -> f.getSeverity() == AccessibilityFinding.Severity.WARNING).count();
        long infos = findings.stream()
                .filter(f -> f.getSeverity() == AccessibilityFinding.Severity.INFO).count();

        Set<String> criteriaWithIssues = findings.stream()
                .filter(f -> f.getSeverity() != AccessibilityFinding.Severity.INFO)
                .map(AccessibilityFinding::getWcagCriterion)
                .collect(Collectors.toSet());

        Map<String, Long> findingsPerCheck = findings.stream()
                .collect(Collectors.groupingBy(AccessibilityFinding::getCheckId, Collectors.counting()));

        this.summary = new Summary(errors, warnings, infos,
                findings.size(), criteriaWithIssues.size(), findingsPerCheck);
    }

    public String toJson() throws IOException {
        buildSummary();
        return MAPPER.writeValueAsString(this);
    }

    // --- Getters & Setters ---

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getSourceApplication() { return sourceApplication; }
    public void setSourceApplication(String sourceApplication) { this.sourceApplication = sourceApplication; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public String getTimestamp() { return timestamp; }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType documentType) { this.documentType = documentType; }
    public MetadataProfile getMetadataProfile() { return metadataProfile; }
    public void setMetadataProfile(MetadataProfile metadataProfile) { this.metadataProfile = metadataProfile; }
    public Summary getSummary() { return summary; }
    public List<AccessibilityFinding> getFindings() { return Collections.unmodifiableList(findings); }

    /**
     * Returns only findings that have a paragraph-level location,
     * suitable for placing annotations.
     */
    public List<AccessibilityFinding> getAnnotatableFindings() {
        return findings.stream()
                .filter(f -> f.getLocation() != null)
                .filter(f -> f.getLocation().getType() != ElementLocation.LocationType.DOCUMENT)
                .filter(f -> f.getLocation().getType() != ElementLocation.LocationType.STYLE)
                .collect(Collectors.toList());
    }

    /**
     * Returns document-level findings (no specific location).
     */
    public List<AccessibilityFinding> getDocumentLevelFindings() {
        return findings.stream()
                .filter(f -> f.getLocation() == null
                        || f.getLocation().getType() == ElementLocation.LocationType.DOCUMENT)
                .collect(Collectors.toList());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Summary {
        private final long errors;
        private final long warnings;
        private final long infos;
        private final long totalFindings;
        private final long wcagCriteriaAffected;
        private final Map<String, Long> findingsPerCheck;

        public Summary(long errors, long warnings, long infos,
                       long totalFindings, long wcagCriteriaAffected,
                       Map<String, Long> findingsPerCheck) {
            this.errors = errors;
            this.warnings = warnings;
            this.infos = infos;
            this.totalFindings = totalFindings;
            this.wcagCriteriaAffected = wcagCriteriaAffected;
            this.findingsPerCheck = findingsPerCheck;
        }

        public long getErrors() { return errors; }
        public long getWarnings() { return warnings; }
        public long getInfos() { return infos; }
        public long getTotalFindings() { return totalFindings; }
        public long getWcagCriteriaAffected() { return wcagCriteriaAffected; }
        public Map<String, Long> getFindingsPerCheck() { return findingsPerCheck; }
    }
}
