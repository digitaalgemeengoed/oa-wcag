package nl.digitaalgemeengoed.wcag;

import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/** Small test documents, made with POI. */
final class Bestanden {

    /** A 1x1 PNG. */
    static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    private Bestanden() {}

    /** A Word document; with title and language set in the way Word does it (core title, docDefaults language). */
    static byte[] docx(boolean metTitelEnTaal, boolean vetteNepkop) throws IOException {
        try (XWPFDocument d = new XWPFDocument(); ByteArrayOutputStream uit = new ByteArrayOutputStream()) {
            if (metTitelEnTaal) {
                d.getProperties().getCoreProperties().setTitle("Raadsvoorstel fietsenstalling");
                XWPFStyles stijlen = d.createStyles();
                var rpr = org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocDefaults.Factory.newInstance();
                rpr.addNewRPrDefault().addNewRPr().addNewLang().setVal("nl-NL");
                stijlen.setStyles(org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles.Factory.newInstance());
                stijlen.getCtStyles().setDocDefaults(rpr);
            }
            XWPFParagraph p = d.createParagraph();
            XWPFRun r = p.createRun();
            if (vetteNepkop) {
                r.setBold(true);
                r.setFontSize(16);
                r.setText("Inleiding");
                d.createParagraph().createRun().setText("Het college stelt de raad voor een krediet beschikbaar te stellen.");
            } else {
                r.setText("Het college stelt de raad voor een krediet beschikbaar te stellen.");
            }
            d.write(uit);
            return uit.toByteArray();
        }
    }

    /** A presentation with one slide without a title, and a picture with or without alternative text. */
    static byte[] pptx(String altTekst) throws IOException {
        try (XMLSlideShow s = new XMLSlideShow(); ByteArrayOutputStream uit = new ByteArrayOutputStream()) {
            POIXMLProperties.CoreProperties kern = s.getProperties().getCoreProperties();
            kern.setTitle("Presentatie");
            XSLFSlide dia = s.createSlide();
            XSLFPictureData data = s.addPicture(PNG, PictureData.PictureType.PNG);
            XSLFPictureShape pic = dia.createPicture(data);
            var cnv = ((org.openxmlformats.schemas.presentationml.x2006.main.CTPicture) pic.getXmlObject()).getNvPicPr().getCNvPr();
            if (altTekst != null) cnv.setDescr(altTekst);
            else if (cnv.isSetDescr()) cnv.unsetDescr();
            s.write(uit);
            return uit.toByteArray();
        }
    }

    /** A workbook with a default sheet name and a picture with or without alternative text. */
    static byte[] xlsx(String altTekst) throws IOException {
        try (XSSFWorkbook w = new XSSFWorkbook(); ByteArrayOutputStream uit = new ByteArrayOutputStream()) {
            XSSFSheet blad = w.createSheet("Sheet1");
            blad.createRow(0).createCell(0).setCellValue("Bedrag");
            int idx = w.addPicture(PNG, XSSFWorkbook.PICTURE_TYPE_PNG);
            XSSFDrawing tekening = blad.createDrawingPatriarch();
            XSSFPicture pic = tekening.createPicture(new XSSFClientAnchor(0, 0, 0, 0, 1, 1, 2, 2), idx);
            var cnv = pic.getCTPicture().getNvPicPr().getCNvPr();
            if (altTekst != null) cnv.setDescr(altTekst);
            else if (cnv.isSetDescr()) cnv.unsetDescr();
            w.write(uit);
            return uit.toByteArray();
        }
    }
}
