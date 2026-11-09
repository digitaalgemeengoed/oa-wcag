package nl.digitaalgemeengoed.wcag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import nl.overheid.wcag.model.AccessibilityReport;
import nl.overheid.wcag.model.DocumentType;
import nl.overheid.wcag.model.MetadataProfile;
import nl.overheid.wcag.runner.CheckRunner;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * De toegankelijkheidscontrole als dienst voor de Office Assistent.
 *
 * <p>Alleen de integratielaag roept haar aan, met de dienstsleutel in de kop
 * {@code x-oa-dienstsleutel}. Er is geen dashboard en geen openbare
 * documentatie-pagina: de dienst hoort niet op internet.
 *
 * <pre>
 *   POST /controle?formaat=docx|xlsx|pptx   (het bestand als lichaam)
 *   GET  /gezondheid
 * </pre>
 *
 * <p>Het bestand blijft alleen in het geheugen en wordt niet bewaard.
 */
public final class Dienst {

    private static final Logger LOG = LoggerFactory.getLogger(Dienst.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    static final String VERSIE = versie();

    private final byte[] sleutel;
    private final long maxBytes;
    private final long tijdslimietMs;
    private final Semaphore plaatsen;
    private final ExecutorService werkers;
    private final CheckRunner runner = new CheckRunner(MetadataProfile.WCAG);
    private HttpServer server;

    public Dienst(String sleutel, long maxBytes, long tijdslimietMs, int gelijktijdig) {
        if (sleutel == null || sleutel.length() < 16) {
            throw new IllegalArgumentException("WCAG_DIENSTSLEUTEL ontbreekt of is korter dan 16 tekens.");
        }
        this.sleutel = sleutel.getBytes(StandardCharsets.UTF_8);
        this.maxBytes = maxBytes;
        this.tijdslimietMs = tijdslimietMs;
        this.plaatsen = new Semaphore(gelijktijdig);
        this.werkers = Executors.newFixedThreadPool(gelijktijdig, r -> {
            Thread t = new Thread(r, "oa-wcag-controle");
            t.setDaemon(true);
            return t;
        });
        beveiligPoi(maxBytes);
    }

    /**
     * Grenzen voor wat POI uitpakt: tegen een "zipbom" (een klein bestand dat
     * uitgepakt gigabytes wordt) en tegen bestanden met duizenden onderdelen.
     */
    static void beveiligPoi(long maxBytes) {
        ZipSecureFile.setMinInflateRatio(0.01);
        ZipSecureFile.setMaxEntrySize(Math.max(100L * 1024 * 1024, 4 * maxBytes));
        ZipSecureFile.setMaxTextSize(50L * 1024 * 1024);
        ZipSecureFile.setMaxFileCount(2000);
        IOUtils.setByteArrayMaxOverride((int) Math.min(Integer.MAX_VALUE, Math.max(200L * 1024 * 1024, 4 * maxBytes)));
    }

    public void start(String host, int poort) throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, poort), 32);
        server.createContext("/", this::behandel);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        LOG.info("oa-wcag {} luistert op {}:{} (hoogstens {} MB, {} s, {} tegelijk)", VERSIE, host, poort,
                maxBytes / (1024 * 1024), tijdslimietMs / 1000, plaatsen.availablePermits());
    }

    public int poort() {
        return server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) server.stop(0);
        werkers.shutdownNow();
    }

    private void behandel(HttpExchange x) throws IOException {
        try (x) {
            String pad = x.getRequestURI().getPath();
            String methode = x.getRequestMethod();
            if ("GET".equals(methode) && "/gezondheid".equals(pad)) {
                stuur(x, 200, Map.of("status", "ok", "versie", VERSIE));
                return;
            }
            if (!toegestaan(x)) {
                stuur(x, 401, fout("geen-toegang", "Alleen de integratielaag mag deze dienst aanroepen."));
                return;
            }
            if ("POST".equals(methode) && "/controle".equals(pad)) {
                controle(x);
                return;
            }
            stuur(x, 404, fout("onbekend", "Deze route bestaat niet."));
        } catch (Exception e) {
            LOG.error("Onverwachte fout", e);
            try {
                stuur(x, 500, fout("intern", "De controle liep vast. Probeer het opnieuw."));
            } catch (Exception ignored) {
                // Antwoord was al (deels) verstuurd
            }
        }
    }

    private boolean toegestaan(HttpExchange x) {
        String gegeven = x.getRequestHeaders().getFirst("x-oa-dienstsleutel");
        return gegeven != null && MessageDigest.isEqual(gegeven.getBytes(StandardCharsets.UTF_8), sleutel);
    }

    private void controle(HttpExchange x) throws IOException {
        DocumentType type = formaat(queryParam(x, "formaat"));
        if (type == null) {
            stuur(x, 400, fout("formaat", "Geef het formaat mee: docx, xlsx of pptx. Andere formaten (zoals odt) eerst omzetten."));
            return;
        }
        String lengte = x.getRequestHeaders().getFirst("content-length");
        byte[] bestand = lengte != null && parseLong(lengte) > maxBytes ? null : leesBegrensd(x.getRequestBody(), maxBytes);
        if (bestand == null) {
            // Wat nog onderweg is, wegwerken (begrensd), anders komt het antwoord niet aan.
            leegRest(x.getRequestBody(), 4 * maxBytes);
            stuur(x, 413, fout("te-groot", "Het bestand is groter dan " + maxBytes / (1024 * 1024) + " MB."));
            return;
        }
        if (bestand.length < 4 || bestand[0] != 'P' || bestand[1] != 'K') {
            stuur(x, 400, fout("geen-office-bestand", "Dit is geen " + type.name().toLowerCase() + "-bestand."));
            return;
        }
        boolean plaats;
        try {
            plaats = plaatsen.tryAcquire(Math.min(tijdslimietMs, 10_000), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plaats = false;
        }
        if (!plaats) {
            stuur(x, 503, fout("druk", "De controle is even te druk. Probeer het zo opnieuw."));
            return;
        }
        long start = System.nanoTime();
        try {
            Future<AccessibilityReport> taak = werkers.submit(() -> runner.check(new ByteArrayInputStream(bestand), "document." + type.getExtension()));
            AccessibilityReport r;
            try {
                r = taak.get(tijdslimietMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                taak.cancel(true);
                LOG.warn("Controle afgebroken na {} ms ({} bytes, {})", tijdslimietMs, bestand.length, type);
                stuur(x, 504, fout("te-lang", "De controle duurde te lang. Het document is mogelijk erg groot of ingewikkeld."));
                return;
            } catch (ExecutionException e) {
                LOG.warn("Bestand niet te lezen ({}, {} bytes): {}", type, bestand.length, e.getCause() == null ? e : e.getCause().toString());
                stuur(x, 422, fout("onleesbaar", "Dit bestand kon niet worden gelezen. Is het beschadigd of beveiligd met een wachtwoord?"));
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stuur(x, 503, fout("druk", "De controle werd onderbroken. Probeer het opnieuw."));
                return;
            }
            Map<String, Object> uit = Vertaling.antwoord(r, type);
            LOG.info("Gecontroleerd: {} ({} kB) in {} ms, {} bevindingen", type, bestand.length / 1024,
                    (System.nanoTime() - start) / 1_000_000, r.getFindings().size());
            stuur(x, 200, uit);
        } finally {
            plaatsen.release();
        }
    }

    static DocumentType formaat(String f) {
        if (f == null) return null;
        return switch (f.toLowerCase()) {
            case "docx" -> DocumentType.DOCX;
            case "xlsx" -> DocumentType.XLSX;
            case "pptx" -> DocumentType.PPTX;
            default -> null;
        };
    }

    private static String queryParam(HttpExchange x, String naam) {
        String q = x.getRequestURI().getRawQuery();
        if (q == null) return null;
        for (String deel : q.split("&")) {
            int i = deel.indexOf('=');
            if (i > 0 && deel.substring(0, i).equals(naam)) return java.net.URLDecoder.decode(deel.substring(i + 1), StandardCharsets.UTF_8);
        }
        return null;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Leest hoogstens max bytes; null als het meer is. */
    static byte[] leesBegrensd(InputStream in, long max) throws IOException {
        ByteArrayOutputStream uit = new ByteArrayOutputStream();
        byte[] buf = new byte[64 * 1024];
        long totaal = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            totaal += n;
            if (totaal > max) return null;
            uit.write(buf, 0, n);
        }
        return uit.toByteArray();
    }

    /** Leest wat er nog in de vraag zit en gooit het weg, tot hoogstens max bytes. */
    static void leegRest(InputStream in, long max) {
        try {
            byte[] buf = new byte[64 * 1024];
            long totaal = 0;
            int n;
            while (totaal < max && (n = in.read(buf)) != -1) totaal += n;
        } catch (IOException ignored) {
            // De verbinding is al weg
        }
    }

    private static Map<String, Object> fout(String code, String melding) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fout", code);
        m.put("melding", melding);
        return m;
    }

    private static void stuur(HttpExchange x, int status, Object lichaam) throws IOException {
        byte[] b = JSON.writeValueAsBytes(lichaam);
        x.getResponseHeaders().set("content-type", "application/json; charset=utf-8");
        x.getResponseHeaders().set("cache-control", "no-store");
        x.getResponseHeaders().set("x-content-type-options", "nosniff");
        x.sendResponseHeaders(status, b.length);
        x.getResponseBody().write(b);
    }

    private static String versie() {
        String v = Dienst.class.getPackage().getImplementationVersion();
        return v != null ? v : "dev";
    }

    private static String omgeving(String naam, String standaard) {
        String w = System.getenv(naam);
        return w == null || w.isBlank() ? standaard : w;
    }

    public static void main(String[] args) throws IOException {
        Dienst d = new Dienst(
                System.getenv("WCAG_DIENSTSLEUTEL"),
                Long.parseLong(omgeving("WCAG_MAX_MB", "25")) * 1024 * 1024,
                Long.parseLong(omgeving("WCAG_TIJDSLIMIET_S", "30")) * 1000,
                Integer.parseInt(omgeving("WCAG_GELIJKTIJDIG", "2")));
        d.start(omgeving("HOST", "0.0.0.0"), Integer.parseInt(omgeving("POORT", "4400")));
        Runtime.getRuntime().addShutdownHook(new Thread(d::stop));
    }
}
