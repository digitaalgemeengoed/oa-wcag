# oa-wcag

De toegankelijkheidscontrole (WCAG 2.1 AA) van Office-bestanden voor de [Office Assistent](https://github.com/digitaalgemeengoed): 34 controles voor `.docx`, `.xlsx` en `.pptx`, als kleine dienst op de server met de assistent-diensten.

Gebaseerd op [EHa-1999/wcag-checker](https://github.com/EHa-1999/wcag-checker) (EUPL 1.2). De controles zelf staan nog onder `nl.overheid.wcag`, zodat verbeteringen in beide richtingen over te nemen zijn; wat voor de assistent nieuw is, staat onder `nl.digitaalgemeengoed.wcag`. Zie [CHANGELOG.md](CHANGELOG.md) voor wat ten opzichte van het origineel is hersteld.

## Wat het naast de assistent doet

De assistent beoordeelt zelf de html die een werkomgeving doorgeeft. Dat werkt overal en de assistent kan het herstellen, maar ziet niet alles. Deze dienst kijkt in het bestand en vindt ook:

- documenttitel en taal (in de eigenschappen en de standaardopmaak);
- kleurcontrast, lettergrootte en tekstafstand;
- leesvolgorde en titels van dia's;
- alt-tekst van afbeeldingen, groepen en grafieken (ook in Excel en PowerPoint);
- tabellen (kopregels, samengevoegde cellen), lijsten, inhoudsopgave, formuliervelden.

ODF (`.odt`, `.ods`, `.odp`) leest de dienst niet: de werkomgeving zet het eerst om (LibreOffice via de brug, Collabora met zijn conversiedienst).

## Gebruik

```
POST /controle?formaat=docx|xlsx|pptx
x-oa-dienstsleutel: <sleutel>
<het bestand als lichaam>
```

Antwoord:

```json
{
  "formaat": "docx",
  "gecontroleerd": ["WCAG 2.1 1.1.1 Niet-tekstuele content", "…"],
  "bevindingen": [
    {
      "id": "wcag-1",
      "module": "toegankelijkheid",
      "ernst": "fout",
      "regel": "WCAG 2.1 1.1.1",
      "melding": "Afbeelding mist een alternatieve tekst (alt-tekst). Bestandsnaam: 'Image1'.",
      "voorstel": { "beschrijving": "Voeg een beschrijvende alt-tekst toe …" },
      "plek": { "beschrijving": "Afbeelding (Image1)", "waar": { "soort": "image" } },
      "controle": "ALT_TEXT_CHECK"
    }
  ]
}
```

Foutantwoorden zijn `{ "fout": "<code>", "melding": "<Nederlandse uitleg>" }`: `geen-toegang` (401), `formaat` of `geen-office-bestand` (400), `te-groot` (413), `onleesbaar` (422, ook bij een zipbom of wachtwoord), `druk` (503), `te-lang` (504).

`GET /gezondheid` geeft `{ "status": "ok", "versie": "…" }`, zonder sleutel.

## Instellingen

| Variabele | Standaard | |
|---|---|---|
| `WCAG_DIENSTSLEUTEL` | (verplicht) | Minstens 16 tekens; dezelfde als in de integratielaag. |
| `POORT` / `HOST` | `4400` / `0.0.0.0` | |
| `WCAG_MAX_MB` | `25` | Grootste bestand. |
| `WCAG_TIJDSLIMIET_S` | `30` | Per controle. |
| `WCAG_GELIJKTIJDIG` | `2` | Controles tegelijk. |

## Bouwen en testen

Java 21 en Maven.

```
./test/maak-lo.sh     # optioneel: een testdocument uit LibreOffice
mvn package           # tests, daarna target/oa-wcag.jar
WCAG_DIENSTSLEUTEL=… java -jar target/oa-wcag.jar
```

Of met Docker: `docker build -t oa/wcag .`

## Licentie

EUPL 1.2, zoals het origineel.
