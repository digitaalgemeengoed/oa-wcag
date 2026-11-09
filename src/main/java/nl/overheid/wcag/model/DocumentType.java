package nl.overheid.wcag.model;

/**
 * Supported document types for WCAG checking.
 */
public enum DocumentType {

    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private final String extension;
    private final String mimeType;

    DocumentType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String getExtension() { return extension; }
    public String getMimeType() { return mimeType; }

    /**
     * Detect document type based on filename.
     *
     * @param filename filename with extension
     * @return the document type
     * @throws IllegalArgumentException if the type is not supported
     */
    public static DocumentType fromFilename(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename is required");
        }
        String lower = filename.toLowerCase().trim();
        if (lower.endsWith(".docx")) return DOCX;
        if (lower.endsWith(".xlsx")) return XLSX;
        if (lower.endsWith(".pptx")) return PPTX;
        throw new IllegalArgumentException(
                "Niet-ondersteund bestandstype: " + filename
                        + ". Ondersteunde formaten: .docx, .xlsx, .pptx");
    }

    /**
     * Check if a filename has a supported type.
     */
    public static boolean isSupported(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase().trim();
        return lower.endsWith(".docx") || lower.endsWith(".xlsx") || lower.endsWith(".pptx");
    }
}
