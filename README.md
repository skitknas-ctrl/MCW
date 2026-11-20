# MCW – Missed Call Widget

En hemskärmswidget för Android som syns ordentligt när du har missat ett samtal.

- Röd med telefonlur och en pil som studsar bort när det finns missade samtal som inte har setts
- Grå och diskret när allt är sett
- Luren ligger ner när widgeten är bredare än hög och står upp när den är högre än bred eller kvadratisk (högst 10 % bredare än hög räknas som kvadratisk)
- Tryck på widgeten öppnar telefonappens lista med senaste samtal (ringda, mottagna, missade) och gör den grå igen
- Blir också grå när samtalen ses direkt i telefonappen
- Uppdateras när samtalsloggen ändras, när ett samtal avslutas och var 15:e minut som reserv
- Behörigheter: läsa samtalsloggen, telefonstatus (för att märka när ett samtal är slut), start vid omstart
- Kräver Android 12 (API 31) eller senare

## Bygga

```
./gradlew assembleRelease
```

Varje push till `main` bygger en APK via GitHub Actions och publicerar den som release (v1.0.<n>), så att Obtainium hittar nya versioner.

## Utgåvor och F-Droid

- Varje push till `main` ger ett testbygge via GitHub Actions (för Obtainium).
- En officiell utgåva har fast `versionCode` och `versionName` i `app/build.gradle.kts` och en tagg `release-<versionName>`
  (nu `release-1.1.0`). F-Droid bygger från den taggen. Ny utgåva: höj båda värdena, lägg till
  `metadata/en-US/changelogs/<versionCode>.txt`, pusha och sätt taggen när bygget gått igenom.
- Texterna som visas på F-Droid ligger i `metadata/en-US/`.

## Licens

GPL-3.0-or-later. Se `LICENSE`.
