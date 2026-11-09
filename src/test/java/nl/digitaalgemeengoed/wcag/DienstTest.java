package nl.digitaalgemeengoed.wcag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class DienstTest {

    static final String SLEUTEL = "wcag-testsleutel-0123456789";
    static Dienst dienst;
    static final HttpClient HTTP = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();
    static final ObjectMapper JSON = new ObjectMapper();

    @BeforeAll
    static void start() throws Exception {
        dienst = new Dienst(SLEUTEL, 2L * 1024 * 1024, 20_000, 2);
        dienst.start("127.0.0.1", 0);
    }

    @AfterAll
    static void stop() {
        dienst.stop();
    }

    record Antwoord(int status, JsonNode lichaam) {}

    static Antwoord vraag(String methode, String pad, byte[] lichaam, String sleutel) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + dienst.poort() + pad));
        if (sleutel != null) b.header("x-oa-dienstsleutel", sleutel);
        b.method(methode, lichaam == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(lichaam));
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Antwoord(r.statusCode(), JSON.readTree(r.body()));
    }

    static List<String> regels(JsonNode antwoord) {
        List<String> uit = new ArrayList<>();
        antwoord.get("bevindingen").forEach(b -> uit.add(b.get("regel").asText() + " " + b.get("ernst").asText() + " " + b.get("melding").asText()));
        return uit;
    }

    @Test
    void gezondheidZonderSleutel() throws Exception {
        Antwoord a = vraag("GET", "/gezondheid", null, null);
        assertEquals(200, a.status());
        assertEquals("ok", a.lichaam().get("status").asText());
    }

    @Test
    void controleAlleenMetSleutel() throws Exception {
        assertEquals(401, vraag("POST", "/controle?formaat=docx", Bestanden.docx(true, false), null).status());
        assertEquals(401, vraag("POST", "/controle?formaat=docx", Bestanden.docx(true, false), "verkeerd").status());
    }

    @Test
    void formaatVerplicht() throws Exception {
        Antwoord a = vraag("POST", "/controle", Bestanden.docx(true, false), SLEUTEL);
        assertEquals(400, a.status());
        assertEquals("formaat", a.lichaam().get("fout").asText());
        assertEquals(400, vraag("POST", "/controle?formaat=odt", Bestanden.docx(true, false), SLEUTEL).status());
    }

    @Test
    void geenOfficeBestand() throws Exception {
        Antwoord a = vraag("POST", "/controle?formaat=docx", "gewoon tekst".getBytes(), SLEUTEL);
        assertEquals(400, a.status());
        assertEquals("geen-office-bestand", a.lichaam().get("fout").asText());
    }

    @Test
    void teGroot() throws Exception {
        byte[] groot = new byte[3 * 1024 * 1024];
        groot[0] = 'P';
        groot[1] = 'K';
        assertEquals(413, vraag("POST", "/controle?formaat=docx", groot, SLEUTEL).status());
    }

    @Test
    void zipbomWordtGeweigerd() throws Exception {
        // 300 MB nullen, ingepakt een paar honderd kB: POI moet dit weigeren.
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(b)) {
            z.putNextEntry(new ZipEntry("[Content_Types].xml"));
            byte[] nul = new byte[1024 * 1024];
            for (int i = 0; i < 300; i++) z.write(nul);
            z.closeEntry();
        }
        Antwoord a = vraag("POST", "/controle?formaat=docx", b.toByteArray(), SLEUTEL);
        assertEquals(422, a.status(), a.lichaam().toString());
        assertFalse(a.lichaam().get("melding").asText().contains("Exception"), "geen technische details naar buiten");
    }

    @Test
    void wordGoedDocumentGeenTitelOfTaalfout() throws Exception {
        Antwoord a = vraag("POST", "/controle?formaat=docx", Bestanden.docx(true, false), SLEUTEL);
        assertEquals(200, a.status(), a.lichaam().toString());
        List<String> r = regels(a.lichaam());
        assertTrue(r.stream().noneMatch(s -> s.startsWith("WCAG 2.1 3.1.1")), "taal staat in docDefaults: " + r);
        assertTrue(r.stream().noneMatch(s -> s.startsWith("WCAG 2.1 2.4.2")), "titel staat in de eigenschappen: " + r);
        assertTrue(a.lichaam().get("gecontroleerd").size() > 10);
        assertEquals("docx", a.lichaam().get("formaat").asText());
    }

    @Test
    void wordZonderTitelEnTaal() throws Exception {
        Antwoord a = vraag("POST", "/controle?formaat=docx", Bestanden.docx(false, true), SLEUTEL);
        assertEquals(200, a.status());
        List<String> r = regels(a.lichaam());
        assertTrue(r.stream().anyMatch(s -> s.startsWith("WCAG 2.1 3.1.1 fout")), r.toString());
        assertTrue(r.stream().anyMatch(s -> s.startsWith("WCAG 2.1 2.4.2")), r.toString());
        // Elke bevinding heeft de vorm van de assistent
        for (JsonNode b : a.lichaam().get("bevindingen")) {
            assertTrue(b.get("id").asText().startsWith("wcag-"));
            assertEquals("toegankelijkheid", b.get("module").asText());
            assertTrue(List.of("fout", "waarschuwing", "advies").contains(b.get("ernst").asText()));
        }
    }

    @Test
    void wordNepkopGevonden() throws Exception {
        Antwoord a = vraag("POST", "/controle?formaat=docx", Bestanden.docx(true, true), SLEUTEL);
        JsonNode kop = null;
        for (JsonNode b : a.lichaam().get("bevindingen")) if (b.get("regel").asText().equals("WCAG 2.1 1.3.1") && b.path("plek").has("fragment")) kop = b;
        assertNotNull(kop, regels(a.lichaam()).toString());
        assertEquals("Inleiding", kop.get("plek").get("fragment").asText());
        assertTrue(kop.get("plek").get("beschrijving").asText().startsWith("Alinea"));
    }

    @Test
    void powerpointAltTekst() throws Exception {
        List<String> zonder = regels(vraag("POST", "/controle?formaat=pptx", Bestanden.pptx(null), SLEUTEL).lichaam());
        List<String> met = regels(vraag("POST", "/controle?formaat=pptx", Bestanden.pptx("Logo van de gemeente"), SLEUTEL).lichaam());
        assertTrue(zonder.stream().anyMatch(s -> s.startsWith("WCAG 2.1 1.1.1 fout")), zonder.toString());
        assertTrue(met.stream().noneMatch(s -> s.startsWith("WCAG 2.1 1.1.1")), met.toString());
        assertTrue(zonder.stream().anyMatch(s -> s.startsWith("WCAG 2.1 2.4.2")), "dia zonder titel: " + zonder);
    }

    @Test
    void excelAltTekstEnBladnaam() throws Exception {
        List<String> zonder = regels(vraag("POST", "/controle?formaat=xlsx", Bestanden.xlsx(null), SLEUTEL).lichaam());
        List<String> met = regels(vraag("POST", "/controle?formaat=xlsx", Bestanden.xlsx("Grafiek van de kosten"), SLEUTEL).lichaam());
        assertTrue(zonder.stream().anyMatch(s -> s.startsWith("WCAG 2.1 1.1.1 fout")), zonder.toString());
        assertTrue(met.stream().noneMatch(s -> s.startsWith("WCAG 2.1 1.1.1")), "met alt-tekst geen melding (het origineel meldde elke afbeelding): " + met);
        assertTrue(zonder.stream().anyMatch(s -> s.contains("Sheet1")), "standaardbladnaam: " + zonder);
    }

    /** Documents made by LibreOffice (as from the bridge): if soffice is not there, skipped. */
    @Test
    void documentUitLibreOffice() throws Exception {
        Path map = Path.of("target", "lo");
        Path docx = map.resolve("uit-libreoffice.docx");
        if (!Files.exists(docx)) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "geen document uit LibreOffice (zie test/maak-lo.sh)");
        }
        Antwoord a = vraag("POST", "/controle?formaat=docx", Files.readAllBytes(docx), SLEUTEL);
        assertEquals(200, a.status());
        List<String> r = regels(a.lichaam());
        assertTrue(r.stream().noneMatch(s -> s.startsWith("WCAG 2.1 3.1.1")), "LibreOffice zet de taal in docDefaults: " + r);
        assertTrue(r.stream().anyMatch(s -> s.startsWith("WCAG 2.1 1.1.1")), "afbeelding zonder alt-tekst: " + r);
    }
}
