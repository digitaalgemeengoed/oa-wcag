package nl.overheid.wcag.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Location information for a finding in the document.
 * Enables mapping findings back to the exact location
 * in the source document, enabling annotations and UI highlighting.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElementLocation {

    public enum LocationType {
        PARAGRAPH,
        TABLE,
        TABLE_CELL,
        IMAGE,
        DOCUMENT,
        HEADER,
        FOOTER,
        STYLE,
        // Spreadsheet (xlsx)
        SHEET,
        SPREADSHEET_CELL,
        CHART,
        // Presentatie (pptx)
        SLIDE,
        SLIDE_ELEMENT
    }

    private LocationType type;

    // Paragraph-based location
    private Integer paragraphIndex;
    private Integer runIndex;
    private String textSnippet;

    // Table-based location
    private Integer tableIndex;
    private Integer rowIndex;
    private Integer cellIndex;

    // Image-based location
    private String imageId;        // rId in de docx relatie
    private String imageName;      // bestandsnaam van de afbeelding

    // Style-based location
    private String styleId;
    private String styleName;

    // Body element index (universal, for ordering within the document)
    private Integer bodyElementIndex;

    // Spreadsheet-based location (xlsx)
    private String sheetName;
    private Integer sheetIndex;
    private String cellReference;   // bijv. "A1", "C5"
    private String chartName;

    // Presentation-based location (pptx)
    private Integer slideNumber;
    private String slideTitle;
    private String shapeName;
    private String shapeType;       // bijv. "Afbeelding", "Tabel", "Tekstvak"

    private ElementLocation() {}

    // --- Factory methods ---

    public static ElementLocation document() {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.DOCUMENT;
        return loc;
    }

    public static ElementLocation paragraph(int bodyElementIndex, int paragraphIndex, String textSnippet) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.PARAGRAPH;
        loc.bodyElementIndex = bodyElementIndex;
        loc.paragraphIndex = paragraphIndex;
        loc.textSnippet = truncateSnippet(textSnippet, 80);
        return loc;
    }

    public static ElementLocation paragraphRun(int bodyElementIndex, int paragraphIndex, int runIndex, String textSnippet) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.PARAGRAPH;
        loc.bodyElementIndex = bodyElementIndex;
        loc.paragraphIndex = paragraphIndex;
        loc.runIndex = runIndex;
        loc.textSnippet = truncateSnippet(textSnippet, 80);
        return loc;
    }

    public static ElementLocation image(int bodyElementIndex, int paragraphIndex, String imageId, String imageName) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.IMAGE;
        loc.bodyElementIndex = bodyElementIndex;
        loc.paragraphIndex = paragraphIndex;
        loc.imageId = imageId;
        loc.imageName = imageName;
        return loc;
    }

    public static ElementLocation table(int bodyElementIndex, int tableIndex) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.TABLE;
        loc.bodyElementIndex = bodyElementIndex;
        loc.tableIndex = tableIndex;
        return loc;
    }

    public static ElementLocation tableCell(int bodyElementIndex, int tableIndex, int rowIndex, int cellIndex, String textSnippet) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.TABLE_CELL;
        loc.bodyElementIndex = bodyElementIndex;
        loc.tableIndex = tableIndex;
        loc.rowIndex = rowIndex;
        loc.cellIndex = cellIndex;
        loc.textSnippet = truncateSnippet(textSnippet, 80);
        return loc;
    }

    public static ElementLocation style(String styleId, String styleName) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.STYLE;
        loc.styleId = styleId;
        loc.styleName = styleName;
        return loc;
    }

    // --- Spreadsheet factory methods ---

    public static ElementLocation sheet(int sheetIndex, String sheetName) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.SHEET;
        loc.sheetIndex = sheetIndex;
        loc.sheetName = sheetName;
        return loc;
    }

    public static ElementLocation spreadsheetCell(int sheetIndex, String sheetName,
                                                   String cellReference, String textSnippet) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.SPREADSHEET_CELL;
        loc.sheetIndex = sheetIndex;
        loc.sheetName = sheetName;
        loc.cellReference = cellReference;
        loc.textSnippet = truncateSnippet(textSnippet, 80);
        return loc;
    }

    public static ElementLocation chart(int sheetIndex, String sheetName, String chartName) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.CHART;
        loc.sheetIndex = sheetIndex;
        loc.sheetName = sheetName;
        loc.chartName = chartName;
        return loc;
    }

    // --- Presentation factory methods ---

    public static ElementLocation slide(int slideNumber, String slideTitle) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.SLIDE;
        loc.slideNumber = slideNumber;
        loc.slideTitle = slideTitle;
        return loc;
    }

    public static ElementLocation slideElement(int slideNumber, String slideTitle,
                                                String shapeName, String shapeType) {
        ElementLocation loc = new ElementLocation();
        loc.type = LocationType.SLIDE_ELEMENT;
        loc.slideNumber = slideNumber;
        loc.slideTitle = slideTitle;
        loc.shapeName = shapeName;
        loc.shapeType = shapeType;
        return loc;
    }

    private static String truncateSnippet(String text, int maxLen) {
        if (text == null) return null;
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    // --- Getters ---

    public LocationType getType() { return type; }
    public Integer getParagraphIndex() { return paragraphIndex; }
    public Integer getRunIndex() { return runIndex; }
    public String getTextSnippet() { return textSnippet; }
    public Integer getTableIndex() { return tableIndex; }
    public Integer getRowIndex() { return rowIndex; }
    public Integer getCellIndex() { return cellIndex; }
    public String getImageId() { return imageId; }
    public String getImageName() { return imageName; }
    public String getStyleId() { return styleId; }
    public String getStyleName() { return styleName; }
    public Integer getBodyElementIndex() { return bodyElementIndex; }
    public String getSheetName() { return sheetName; }
    public Integer getSheetIndex() { return sheetIndex; }
    public String getCellReference() { return cellReference; }
    public String getChartName() { return chartName; }
    public Integer getSlideNumber() { return slideNumber; }
    public String getSlideTitle() { return slideTitle; }
    public String getShapeName() { return shapeName; }
    public String getShapeType() { return shapeType; }

    /**
     * Returns a human-readable location description,
     * suitable for use in annotations/comments.
     */
    public String toDisplayString() {
        switch (type) {
            case DOCUMENT:
                return "Document (algemeen)";
            case PARAGRAPH:
                return "Alinea " + (paragraphIndex + 1)
                        + (textSnippet != null ? ": \"" + textSnippet + "\"" : "");
            case IMAGE:
                return "Afbeelding" + (imageName != null ? " '" + imageName + "'" : "")
                        + " in alinea " + (paragraphIndex + 1);
            case TABLE:
                return "Tabel " + (tableIndex + 1);
            case TABLE_CELL:
                return "Tabel " + (tableIndex + 1)
                        + ", rij " + (rowIndex + 1)
                        + ", kolom " + (cellIndex + 1)
                        + (textSnippet != null ? ": \"" + textSnippet + "\"" : "");
            case STYLE:
                return "Stijl '" + (styleName != null ? styleName : styleId) + "'";
            // Spreadsheet types
            case SHEET:
                return "Werkblad '" + (sheetName != null ? sheetName : "Blad " + (sheetIndex + 1)) + "'";
            case SPREADSHEET_CELL:
                return "Werkblad '" + (sheetName != null ? sheetName : "Blad " + (sheetIndex + 1))
                        + "', cel " + (cellReference != null ? cellReference : "?")
                        + (textSnippet != null ? ": \"" + textSnippet + "\"" : "");
            case CHART:
                return "Grafiek" + (chartName != null ? " '" + chartName + "'" : "")
                        + " in werkblad '" + (sheetName != null ? sheetName : "Blad " + (sheetIndex + 1)) + "'";
            // Presentation types
            case SLIDE:
                return "Dia " + slideNumber
                        + (slideTitle != null ? " ('" + slideTitle + "')" : "");
            case SLIDE_ELEMENT:
                return "Dia " + slideNumber
                        + (shapeType != null ? ", " + shapeType : "")
                        + (shapeName != null ? " '" + shapeName + "'" : "");
            default:
                return "Onbekende locatie";
        }
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}
