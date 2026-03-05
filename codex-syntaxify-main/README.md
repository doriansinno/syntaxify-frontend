# Syntaxify Monorepo

Syntaxify ist eine fokussierte Lern-Web-App für einfache Programmiergrundlagen.

## Projektstruktur

```text
.
├── backend/   # Spring Boot API (Java 17)
├── frontend/  # Statisches HTML/CSS/JS-Frontend (keine Node-Tools)
```

## Lokales Setup

### 1) Backend starten

Das Maven-Build ist so konfiguriert, dass es den Inhalt des `frontend/`-Ordners
als statische Ressourcen in `static/` kopiert. Beim Starten des Backends
liefert Spring Boot diese Dateien automatisch aus, deshalb genügt ein
single command:

```bash
cd backend
export OPENAI_API_KEY="<dein_key>"
# bei Bedarf andere Origins anpassen, z.B. http://localhost:8000
export ALLOWED_ORIGINS="*"

mvn spring-boot:run
```

Backend (inklusive Frontend) läuft unter `http://localhost:8080` (oder
der durch `PORT` gesetzte Wert). öffne diese Adresse im Browser, um das
neue statische Frontend zu sehen.

Health-Check:

```bash
curl http://localhost:8080/health
```

### 2) Frontend starten

Das Frontend besteht aus reinen HTML-, CSS- und JavaScript-Dateien im Verzeichnis `frontend`.

- **Einfach vom Backend ausliefern lassen:** Kopiere die Inhalte nach `backend/src/main/resources/static` oder konfiguriere das Backend so, dass es `frontend/` direkt bedient. Nach dem Start des Backends ist die UI unter `http://localhost:8080` erreichbar.

- **Oder lokalen Mini‑Server nutzen:** Wechsle in den Ordner und starte z.B.: `python -m http.server 8000` oder `npx http-server .`. Dann läuft die UI z.B. unter `http://localhost:8000` und sendet API-Requests an `http://localhost:8080/api/translate`.


## API

### GET /health

Antwort:

```json
{ "status": "ok" }
```

### POST /api/translate

Request:

```json
{
  "input": "Erstelle eine Schleife von 1 bis 5 und gib jede Zahl aus",
  "sourceLanguage": "de",
  "targetLanguage": "python"
}
```

Response:

```json
{
  "translatedCode": "for i in range(1, 6):\n    print(i)",
  "explanation": "Eine for-Schleife läuft von 1 bis 5 und gibt jede Zahl aus.",
  "warnings": [],
  "meta": {
    "sourceLanguage": "de",
    "targetLanguage": "python",
    "model": "gpt-4o-mini",
    "durationMs": 123
  }
}
```

## Laufzeit-Ausführung im Browser

- **JavaScript:** direkte Ausführung mit `new Function(...)` im Frontend.
- **Python:** Ausführung über **Pyodide** im Browser (WASM), damit keine zusätzliche Backend-Sandbox nötig ist.

## Scope-Guard (wichtige Begrenzung)

Syntaxify erlaubt nur:
- Variablen, primitive Datentypen, Listen/Arrays
- Funktionen + Parameter
- if/else, for, while
- print/console.log
- einfache Rechen- und Vergleichsoperatoren

Nicht unterstützt:
- OOP/Klassen, Vererbung, Rekursion
- DB/SQL/API/Filezugriffe
- Threads/Async
- Framework-spezifischer Code

Out-of-scope Anfragen liefern gezielte Warnung statt halluziniertem Code.

## Render Deployment

### Warum diese Trennung?
- **Frontend als Static Site:** React/Vite wird zu statischen Dateien gebaut (`dist/`), ideal für CDN-Auslieferung.
- **Backend als Web Service:** Spring Boot braucht laufenden Prozess für REST API.

### Backend (Render Web Service)

- **Root Directory:** `backend`
- **Environment:** Java
- **Build Command:**
  ```bash
  mvn clean package -DskipTests
  ```
- **Start Command:**
  ```bash
  java -Dserver.port=$PORT -jar target/syntaxify-backend-0.0.1-SNAPSHOT.jar
  ```
- **Environment Variables:**
  - `OPENAI_API_KEY=<dein key>`
  - `OPENAI_MODEL=gpt-4o-mini` (optional)
  - `ALLOWED_ORIGINS=https://<dein-frontend>.onrender.com`

### Frontend (Render Static Site)

- **Root Directory:** `frontend`
- **Build Command:**
  ```bash
  npm ci && npm run build
  ```
- **Publish Directory:**
  ```text
  dist
  ```
- **Environment Variables:**
  - `VITE_API_BASE_URL=https://<dein-backend>.onrender.com`

### Post-Deploy Test

1. Backend Health prüfen:
   ```bash
   curl https://<dein-backend>.onrender.com/health
   ```
2. Frontend öffnen und Beispiel senden.
3. In DevTools prüfen, ob `POST /api/translate` erfolgreich ist.

## Troubleshooting

1. **Render startet JAR nicht**
   - Prüfe Dateiname in Start Command (`target/...jar`).
   - Stelle sicher, dass Build erfolgreich war.

2. **CORS Fehler**
   - `ALLOWED_ORIGINS` muss exakt die Frontend-URL enthalten.
   - Mehrere Origins mit Komma trennen.

3. **Backend antwortet nicht**
   - Prüfe Render Logs.
   - Prüfe `OPENAI_API_KEY` und ob Service auf `$PORT` hört.

4. **Frontend findet Backend nicht**
   - `VITE_API_BASE_URL` gesetzt?
   - Nach Änderung neue Frontend-Deploy auslösen.

5. **mvnw permission issue**
   - Dieses Setup nutzt `mvn`, nicht `./mvnw`.
   - Falls Wrapper ergänzt wird: `chmod +x mvnw`.

6. **Falscher Port lokal**
   - Backend nutzt `PORT` oder fallback `8080`.
   - Frontend muss auf die gleiche URL zeigen (`VITE_API_BASE_URL`).

7. **OPENAI_API_KEY fehlt**
   - API antwortet mit 500 und Hinweis auf fehlenden Key.
   - Variable lokal/Render setzen und neu starten.
