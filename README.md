# TIDE - Java IDE

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-100%25-orange)](https://www.java.com/)
[![Version](https://img.shields.io/badge/Version-latest-blue)](https://github.com/Thillager/TIDE/releases/latest)

TIDE ist eine **leichte, benutzerfreundliche Java-IDE (Integrated Development Environment)**, die speziell für Java-Entwicklung entwickelt wurde. Sie bietet alle wesentlichen Funktionen für das Schreiben, Kompilieren und Ausführen von Java-Programmen – ohne die Komplexität größerer IDEs wie Eclipse oder IntelliJ IDEA.

## Features

- ✅ **Projektmanagement** - Einfache Verwaltung von Java-Projekten
- ✅ **Code-Editor** - Syntax-Highlighting und Code-Bearbeitung
- ✅ **Compiler-Integration** - Direktes Kompilieren von Java-Code
- ✅ **Leichtgewicht** - Schnelle Performance und geringer Ressourcenverbrauch
- ✅ **Plattformunabhängig** - Läuft auf Windows, Linux und macOS (alle Java-unterstützenden Systeme)
- ✅ **GUI mit Java Swing** - Native und responsive Benutzeroberfläche
- ✅ **Update Button** - Weder nerviges neu installieren bei neuen Versionen, noch automatische updates mit eventueller malware

## Anforderungen

- **Java Runtime Environment (JRE) 25 oder höher**
     - Oder die .msi/.deb Installer nutzen
- **Mindestens 750 MB RAM**
- **50 MB freier Festplattenspeicher**

## Installation und Verwendung 

### Option 1: Installer

#### Linux:

1. .deb aus dem letzten release herunterladen.

2. Installieren:
   ```bash
   sudo apt install ./dateiname.deb
   ```

#### Windows:

1. .msi oder .exe (versionsabhängig) Installer aus dem letzten release herunterladen

2. Per doppelklick ausführen.

### Option 2: Vorkompiliertes JAR ausführen

1. Stelle sicher, dass Java auf deinem System installiert ist:
   ```bash
   java -version
   ```

2. Führe die JAR-Datei aus:
   ```bash
   java -jar TIDE.jar
   ```

3. Die TIDE IDE wird sich öffnen und ist bereit zur Verwendung.

### Option 3: Aus dem Source Code kompilieren

1. Klone das Repository:
   ```bash
   git clone https://github.com/Thillager/TIDE.git
   cd TIDE
   ```

2. Kompiliere den Source Code:
   ```bash
   javac -d bin src/main/java/**/*.java
   ```

3. Erstelle eine ausführbare JAR-Datei (optional):
   ```bash
   jar cvfe TIDE.jar TIDE -C bin .
   ```

4. Führe die kompilierte Version aus:
   ```bash
   java -cp bin TIDE
   ```

## Wie funktioniert TIDE?

### Architektur-Übersicht

```
┌─────────────────────────────────────────┐
│         TIDE Benutzeroberfläche          │
│  (Editor, Menüs, Projektbaum, Konsole)  │
└────────────────────┬────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
   ┌────▼─────┐ ┌───▼────┐ ┌────▼─────┐
   │  Editor  │ │Compiler│ │Execution │
   │ Modul    │ │Modul   │ │Modul     │
   └──────────┘ └────────┘ └──────────┘
        │            │            │
        └────────────┼────────────┘
                     │
          ┌──────────▼──────────┐
          │  Java-Dateisystem   │
          │  (.java, .class)    │
          └─────────────────────┘
```

### Workflow-Beispiel

1. **Projekt erstellen**: Starte TIDE und erstelle ein neues Projekt über das Menü
2. **Code schreiben**: Schreibe deine Java-Klassen im integrierten Editor
3. **Kompilieren**: Nutze den Build-Button oder das Menü zum Kompilieren
4. **Ausführen**: Führe dein Programm direkt aus TIDE heraus aus
5. **Debuggen**: Sieh dir Compiler-Fehler in der Konsole an

## Projektstruktur

```
TIDE/
├── src/                      # Quellcode
│   └── main/
│       └── java/             # Java-Source-Dateien
├── libs/                      # Externe Bibliotheken
├── production/                # Produktions-Artefakte
├── T.xml                      # Projektkonfigurationi
```

## Konfiguration

Die Datei `T.xml` enthält die Projektkonfiguration:

```xml
<project>
  <mainClass>TIDE</mainClass>      <!-- Hauptklasse zum Ausführen -->
  <appName>TIDE</appName>          <!-- Anwendungsname -->
  <version>1.0.0</version>         <!-- Versions-String -->
</project>
```

Du kannst diese Datei anpassen, um verschiedene Konfigurationen für dein Projekt zu setzen.
Oder du nutzt TBuild, dass sich per TBuild knopf herunterladen und ausführen lässt und die T.xml dann grafisch bearbeiten kann.

## Beispiel: Erstes Programm mit TIDE

### Schritt 1: Neues Projekt erstellen
Starte TIDE und gehe zu **File → New Project** → Gib dem Projekt einen Namen

### Schritt 2: Neue Java-Klasse erstellen
Gehe zu **File → New Class** und erstelle eine Klasse namens `HelloWorld`

### Schritt 3: Code schreiben
```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Willkommen bei TIDE!");
    }
}
```

### Schritt 4: Kompilieren und Ausführen
- Drücke **Ctrl+B** zum Kompilieren
- Drücke **Ctrl+R** zum Ausführen oder klicke auf den **Run**-Button

## Updates

### Häufigkeit
- Es kommen Updates wann immer ich Zeit finde, Ideen habe oder Fehler auftreten

### Wie installiere ich sie?
- TIDE als Administrator starten
- Auf den "über" button klicken
- Auf den "Nach Updates suchen" button klicken
- Installieren
- Kurz warten (Bis das desktop icon neu lädt)
- Starten

## Troubleshooting

### Problem: "Java nicht gefunden"
**Lösung**: Installiere Java Runtime Environment (JRE) von [java.com](https://www.java.com)

### Problem: JAR-Datei lässt sich nicht ausführen
**Lösung**: 
```bash
# Überprüfe Java-Version
java -version

# Führe mit explizitem Pfad aus
java -jar /pfad/zu/TIDE.jar
```

### Problem: Compiler-Fehler trotz korrektem Code
**Lösung**: 
- Überprüfe deine Java-Syntax
- Stelle sicher, dass alle Klassen in den korrekten Verzeichnissen sind
- Schaue in die Fehlerausgabe in der Konsole

## 📖 Dokumentation und Links

- **Original tIDE Projekt**: https://sites.google.com/site/tidejava/
- **Java Dokumentation**: https://docs.oracle.com/en/java/
- **GitHub Repository**: https://github.com/Thillager/TIDE

## 📝 Lizenz

Dieses Projekt ist unter der **MIT-Lizenz** lizenziert. Siehe [LICENSE](LICENSE) für Details.

## 🤝 Beitragen

Beiträge sind willkommen! Um beizutragen:

1. Forke das Repository
2. Erstelle einen Feature-Branch (`git checkout -b feature/AmazingFeature`)
3. Committe deine Änderungen (`git commit -m 'Add some AmazingFeature'`)
4. Pushe zu dem Branch (`git push origin feature/AmazingFeature`)
5. Öffne einen Pull Request

## ❓ Fragen und Support

Falls du Fragen oder Probleme hast:
- Öffne ein [GitHub Issue](https://github.com/Thillager/TIDE/issues)
- Sieh dir existierende Issues an für häufig gestellte Fragen

---
**Maintainer:** [@Thillager](https://github.com/Thillager)

Viel Erfolg beim Programmieren mit TIDE! 🎉
