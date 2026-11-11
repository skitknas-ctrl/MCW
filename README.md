# MCW – Missed Call Widget

En hemskärmswidget för Android som syns ordentligt när du har missat ett samtal.

- Röd med telefonlur och en pil som studsar bort när det finns missade samtal som inte har setts
- Grå och diskret när allt är sett
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
