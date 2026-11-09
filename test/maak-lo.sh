#!/bin/sh
# Maakt een Word-document met LibreOffice (zoals de extensie dat via de brug doet),
# voor de test DienstTest.documentUitLibreOffice. Zonder soffice slaat die test zich over.
set -e
cd "$(dirname "$0")/.."
mkdir -p target/lo
cat > target/lo/uit-libreoffice.html <<'HTML'
<!doctype html><html lang="nl"><head><meta charset="utf-8"><title>Raadsvoorstel</title></head>
<body><h1>Raadsvoorstel fietsenstalling</h1>
<p>Het college stelt de raad voor een krediet beschikbaar te stellen.</p>
<p><img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==" width="40" height="40"></p>
</body></html>
HTML
soffice --headless --convert-to 'docx:MS Word 2007 XML' --outdir target/lo target/lo/uit-libreoffice.html >/dev/null 2>&1
ls -la target/lo/uit-libreoffice.docx
