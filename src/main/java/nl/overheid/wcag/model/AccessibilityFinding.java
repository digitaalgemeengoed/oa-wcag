package nl.overheid.wcag.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * An individual accessibility finding, including location information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessibilityFinding {

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    private final String checkId;
    private final Severity severity;
    private final String wcagCriterion;
    private final String wcagLevel;
    private final String message;
    private final String suggestion;
    private final ElementLocation location;

    public AccessibilityFinding(String checkId, Severity severity, String wcagCriterion,
                                 String wcagLevel, String message, String suggestion,
                                 ElementLocation location) {
        this.checkId = checkId;
        this.severity = severity;
        this.wcagCriterion = wcagCriterion;
        this.wcagLevel = wcagLevel;
        this.message = message;
        this.suggestion = suggestion;
        this.location = location;
    }

    public String getCheckId() { return checkId; }
    public Severity getSeverity() { return severity; }
    public String getWcagCriterion() { return wcagCriterion; }
    public String getWcagLevel() { return wcagLevel; }
    public String getMessage() { return message; }
    public String getSuggestion() { return suggestion; }
    public ElementLocation getLocation() { return location; }

    /**
     * Formats the finding as annotation text for a Word comment.
     */
    public String toAnnotationText() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity).append("] ");
        sb.append("WCAG ").append(wcagCriterion);
        if (wcagLevel != null) {
            sb.append(" (Niveau ").append(wcagLevel).append(")");
        }
        sb.append("\n\n");
        sb.append(message);
        if (suggestion != null && !suggestion.isEmpty()) {
            sb.append("\n\nAanbeveling: ").append(suggestion);
        }
        return sb.toString();
    }

    // --- Builder ---

    public static Builder builder(String checkId) {
        return new Builder(checkId);
    }

    public static class Builder {
        private final String checkId;
        private Severity severity = Severity.WARNING;
        private String wcagCriterion;
        private String wcagLevel;
        private String message;
        private String suggestion;
        private ElementLocation location;

        Builder(String checkId) {
            this.checkId = checkId;
        }

        public Builder severity(Severity severity) { this.severity = severity; return this; }
        public Builder error() { this.severity = Severity.ERROR; return this; }
        public Builder warning() { this.severity = Severity.WARNING; return this; }
        public Builder info() { this.severity = Severity.INFO; return this; }
        public Builder wcag(String criterion, String level) {
            this.wcagCriterion = criterion;
            this.wcagLevel = level;
            return this;
        }
        public Builder message(String message) { this.message = message; return this; }
        public Builder suggestion(String suggestion) { this.suggestion = suggestion; return this; }
        public Builder location(ElementLocation location) { this.location = location; return this; }

        public AccessibilityFinding build() {
            return new AccessibilityFinding(checkId, severity, wcagCriterion,
                    wcagLevel, message, suggestion, location);
        }
    }
}
