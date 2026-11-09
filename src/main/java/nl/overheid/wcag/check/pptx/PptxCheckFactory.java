package nl.overheid.wcag.check.pptx;

import nl.overheid.wcag.model.MetadataProfile;

/**
 * Factory for presentation checks.
 */
public final class PptxCheckFactory {

    private PptxCheckFactory() {}

    public static PresentationCheck documentTitleCheck() { return new PptxDocumentTitleCheckImpl(); }
    public static PresentationCheck documentLanguageCheck() { return new PptxDocumentLanguageCheckImpl(); }
    public static PresentationCheck metadataCheck(MetadataProfile profile) { return new PptxMetadataCheckImpl(profile); }
    public static PresentationCheck slideTitleCheck() { return new PptxSlideTitleCheckImpl(); }
    public static PresentationCheck altTextCheck() { return new PptxAltTextCheckImpl(); }
    public static PresentationCheck readingOrderCheck() { return new PptxReadingOrderCheckImpl(); }
    public static PresentationCheck colorContrastCheck() { return new PptxColorContrastCheckImpl(); }
    public static PresentationCheck tableCheck() { return new PptxTableCheckImpl(); }
    public static PresentationCheck linkTextCheck() { return new PptxLinkTextCheckImpl(); }
}
