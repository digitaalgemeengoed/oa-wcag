package nl.overheid.wcag.check;

import org.apache.poi.xwpf.usermodel.*;

import java.util.*;

/**
 * Pre-computed context with indices for the document.
 * Enables fast lookup of paragraph index, table index, and
 * body element index for location information.
 */
public class DocumentContext {

    private final XWPFDocument document;

    // Body element index lookup
    private final Map<IBodyElement, Integer> bodyElementIndexMap = new IdentityHashMap<>();

    // Paragraph index (across all paragraphs, not body elements)
    private final Map<XWPFParagraph, Integer> paragraphIndexMap = new IdentityHashMap<>();

    // Table index
    private final Map<XWPFTable, Integer> tableIndexMap = new IdentityHashMap<>();

    // Paragraph -> body element index
    private final Map<XWPFParagraph, Integer> paragraphBodyElementIndexMap = new IdentityHashMap<>();

    public DocumentContext(XWPFDocument document) {
        this.document = document;
        buildIndices();
    }

    private void buildIndices() {
        List<IBodyElement> bodyElements = document.getBodyElements();
        int paragraphCounter = 0;
        int tableCounter = 0;

        for (int i = 0; i < bodyElements.size(); i++) {
            IBodyElement element = bodyElements.get(i);
            bodyElementIndexMap.put(element, i);

            if (element instanceof XWPFParagraph) {
                XWPFParagraph para = (XWPFParagraph) element;
                paragraphIndexMap.put(para, paragraphCounter);
                paragraphBodyElementIndexMap.put(para, i);
                paragraphCounter++;
            } else if (element instanceof XWPFTable) {
                XWPFTable table = (XWPFTable) element;
                tableIndexMap.put(table, tableCounter);
                tableCounter++;
            }
        }
    }

    public int getBodyElementIndex(IBodyElement element) {
        return bodyElementIndexMap.getOrDefault(element, -1);
    }

    public int getParagraphIndex(XWPFParagraph paragraph) {
        return paragraphIndexMap.getOrDefault(paragraph, -1);
    }

    public int getParagraphBodyElementIndex(XWPFParagraph paragraph) {
        return paragraphBodyElementIndexMap.getOrDefault(paragraph, -1);
    }

    public int getTableIndex(XWPFTable table) {
        return tableIndexMap.getOrDefault(table, -1);
    }

    public int getTableBodyElementIndex(XWPFTable table) {
        return bodyElementIndexMap.getOrDefault(table, -1);
    }

    public XWPFDocument getDocument() {
        return document;
    }

    public int getTotalParagraphs() {
        return paragraphIndexMap.size();
    }

    public int getTotalTables() {
        return tableIndexMap.size();
    }
}
