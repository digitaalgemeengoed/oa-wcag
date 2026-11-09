package nl.overheid.wcag.check.xlsx;

import nl.overheid.wcag.model.MetadataProfile;

/**
 * Factory for spreadsheet checks.
 *
 * <p>Gebruik:
 * <pre>
 *   SpreadsheetCheck metaCheck = XlsxCheckFactory.metadataCheck(MetadataProfile.MDTO_BASIS);
 *   SpreadsheetCheck sheetCheck = XlsxCheckFactory.sheetNameCheck();
 * </pre>
 */
public final class XlsxCheckFactory {

    private XlsxCheckFactory() {}

    public static SpreadsheetCheck documentTitleCheck() { return new XlsxDocumentTitleCheckImpl(); }
    public static SpreadsheetCheck documentLanguageCheck() { return new XlsxDocumentLanguageCheckImpl(); }
    public static SpreadsheetCheck metadataCheck(MetadataProfile profile) { return new XlsxMetadataCheckImpl(profile); }
    public static SpreadsheetCheck sheetNameCheck() { return new XlsxSheetNameCheckImpl(); }
    public static SpreadsheetCheck tableStructureCheck() { return new XlsxTableStructureCheckImpl(); }
    public static SpreadsheetCheck altTextCheck() { return new XlsxAltTextCheckImpl(); }
    public static SpreadsheetCheck colorContrastCheck() { return new XlsxColorContrastCheckImpl(); }
    public static SpreadsheetCheck textSpacingCheck() { return new XlsxTextSpacingCheckImpl(); }
    public static SpreadsheetCheck linkTextCheck() { return new XlsxLinkTextCheckImpl(); }
}
