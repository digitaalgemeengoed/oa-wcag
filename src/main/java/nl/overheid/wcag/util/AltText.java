package nl.overheid.wcag.util;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

import javax.xml.namespace.QName;

/**
 * Reads the alternative text (the "descr" attribute of the non-visual drawing
 * properties, {@code cNvPr}) of a shape, picture or group in DrawingML. POI has
 * no accessor for it on every shape type, so this reads the XML directly.
 *
 * <p>Looks only at the shape's own {@code cNvPr} (child of its {@code nv*Pr}
 * element), not at the shapes inside a group.
 */
public final class AltText {

    private AltText() {}

    /** The alternative text, or null when none is set. Also counts "decorative" as set. */
    public static String of(XmlObject shape) {
        if (shape == null) return null;
        try (XmlCursor c = shape.newCursor()) {
            if (!c.toFirstChild()) return null;
            do {
                if (c.getName() != null && c.getName().getLocalPart().startsWith("nv")) {
                    try (XmlCursor d = c.getObject().newCursor()) {
                        if (!d.toFirstChild()) continue;
                        do {
                            if (d.getName() != null && "cNvPr".equals(d.getName().getLocalPart())) {
                                String descr = d.getAttributeText(new QName("descr"));
                                if (descr != null && !descr.isBlank()) return descr;
                                if (isDecorative(d.getObject())) return "";
                                return null;
                            }
                        } while (d.toNextSibling());
                    }
                }
            } while (c.toNextSibling());
        }
        return null;
    }

    /** True when the shape is marked as decorative (Office: adec:decorative val="1"). */
    public static boolean isDecorative(XmlObject cNvPr) {
        String xml = cNvPr.xmlText();
        return xml.contains("decorative") && xml.matches("(?s).*decorative[^>]*val=\"(1|true)\".*");
    }

    /** True when an alternative text is set, or the shape is marked as decorative. */
    public static boolean present(XmlObject shape) {
        return of(shape) != null;
    }
}
