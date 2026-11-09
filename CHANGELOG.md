# Wijzigingen

## 0.1.0 — 2026-11-09

Eerste versie: de controles van [EHa-1999/wcag-checker](https://github.com/EHa-1999/wcag-checker) (commit `65fb0f7`) als kleine dienst voor de Office Assistent.

- **Dienst** zonder Spring en zonder NiFi: `POST /controle?formaat=docx|xlsx|pptx` met het bestand als lichaam, `GET /gezondheid`. Alleen met de dienstsleutel (`x-oa-dienstsleutel`); geen dashboard of openbare API-documentatie. Ongeveer 120 MB geheugen.
- **Antwoord in de vorm van de assistent** (oa-contract `Bevinding`): ernst fout/waarschuwing/advies, regel "WCAG 2.1 x.y.z", melding, voorstel, en een plek met een leesbare beschrijving en het tekstfragment om op te zoeken.
- **Afgeschermd:** grens op de grootte (standaard 25 MB), op wat POI uitpakt (tegen zipbommen), een tijdslimiet per controle (30 s), hoogstens 2 controles tegelijk. Geen technische details in foutmeldingen naar buiten; het bestand wordt niet bewaard.
- **Hersteld ten opzichte van het origineel** (dat niet compileerde en niet startte):
  - de code bouwt nu tegen Apache POI 5.5.1 (volledig OOXML-schema): taal, tekstafstand, inhoudsbesturingselementen, alt-tekst in PowerPoint en Excel, kleur in PowerPoint;
  - een dubbele waarde in de vage linkteksten liet de Word-controle bij het starten vastlopen;
  - de taal van een Word-document wordt nu ook gevonden waar Word en LibreOffice haar zetten (standaardopmaak, `w:docDefaults`), in plaats van "taal niet ingesteld" bij bijna elk document;
  - Excel meldde elke afbeelding; nu alleen die zonder alternatieve tekst;
  - de kleurcontrole in PowerPoint brak af na de eerste dia met een melding.
- **Tests** met bestanden van POI en van LibreOffice, ook voor sleutel, grenzen en zipbom.
