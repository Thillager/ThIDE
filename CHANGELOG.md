# Changelog - TIDE Java IDE

Alle wesentlichen Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

---

## [1.8.0] - 2026-05-08

### 🔧 Bugfixes & Verbesserungen
- **Vereinfachte Update-Installation (Windows)**: MSI-Installer-Prozess wurde optimiert
- **Stabilitätsverbesserungen**: Fehlerbehandlung beim Update-Download verbessert

---

## [1.7.0] - 2026-05-07

### 🐛 Bugfixes
- **Update-Mechanismus überarbeitet**: Windows Update-Skript nun robuster und fehlertoleranter
- **Fehlerausgabe verbessert**: Bessere Fehlermeldungen bei Update-Installation

---

## [1.6.0] - 2026-05-07

### ✨ Neue Features
- **Update-System**: Prüfung auf neuere Versionen direkt von GitHub
  - "Nach Updates suchen" Button im Über-Dialog
  - Automatischer Download und Installation von Updates
  - Unterstützung für Windows (.msi) und Linux (.deb)
  
- **Über-Dialog**: Neue "Über TIDE" Schaltfläche mit Versionsinformationen und GitHub-Link

### 🎨 UI/UX Verbesserungen
- **Besserer File-Tree Context-Menu**: Code-Refactoring für bessere Wartbarkeit
- **Toolbar-Layout**: Zusätzlicher Button für Über-Dialog
- **Titel-Anzeige**: App-Version wird jetzt im Fenster-Titel angezeigt

### 🔧 Code-Qualität
- **Umfangreicher Refactor des File-Tree Popup-Menüs**: Doppelter Code eliminiert
- **Bessere Codierstandards**: Formatierung und Struktur verbessert
- **README.md**: Detaillierte Dokumentation hinzugefügt

### 📦 Zusätzlich
- **MIT-Lizenz**: LICENSE-Datei hinzugefügt

---

## [1.5.0] - 2026-05-06

### ✨ Neue Features
- **Suchfunktion**: Ctrl+F zum Öffnen/Schließen des Suchbereichs
  - "Abwärts" und "Aufwärts" Buttons zum Navigieren durch Suchergebnisse
  - "Groß/Klein" Checkbox für Case-Sensitive Suche
  - Visuelle Suchleiste am oberen Rand des Editors

### 🎨 UI/UX Verbesserungen
- **Maximized Window**: Hauptfenster startet jetzt maximiert (vollständiger Bildschirm)
- **Editor Container**: Suchleiste ist jetzt oben im Editor-Container integriert
- **Tastenkombinationen erweitert**: Ctrl+S zum Speichern und Ctrl+F zum Suchen

### 🔧 Interne Änderungen
- **RSyntaxTextArea Integration**: SearchEngine für vollständige Suche-Funktionalität
- **Dynamic UI Updates**: Bessere Handhabung von UI-Komponenten
- **FileTree Refactoring**: Popup-Menu-Logik in separate Methoden ausgelagert

---

## [1.4.0] - 2026-05-05

### 🔄 Änderungen
- **Versionsnummer Update**: T.xml aktualisiert auf 1.4.0
- **Build-Artefakte**: Neue Binärdateien (TIDE.jar)

---

## [1.3.0] - 2026-05-05

### ✨ Neue Features
- **Verbessertes File-Tree Kontextmenü**: 
  - Neue Datei erstellen (Neue Datei)
  - Neuen Ordner erstellen (Neuer Ordner)
  - Dateien kopieren/ausschneiden/einfügen (Clipboard)
  - Dateien löschen mit Bestätigung
  - Dateien umbenennen
  - Im Explorer öffnen
  - Aktualisieren-Button

### 🎨 UI/UX Verbesserungen
- **Maximized Window**: Fenster öffnet jetzt in vollem Fenster-Modus
- **Clipboard-System**: Implementierung eines einfachen Clipboard für Datei-Operationen
- **Bessere Event-Handling**: mousePressed und mouseReleased für Popup-Menus

---

## [1.2.0] - 2026-05-02

### 🔧 Bugfixes
- **Release-Workflow optimiert**: Automatische Artifact-Verwaltung für Fat JAR
- **Dependency Handling**: Explizites Ausschließen von TBuild.jar bei Release
- **Build-Stabilitäten**: Fallback-Handling für dynamische JAR-Namen

### 📦 Änderungen
- **TBuild Integration**: Automatisches Download von TBuild.jar falls nicht vorhanden
- **Artifacts**: Korrekte Linux- und Windows-Installer-Bereitstellung

---

## [1.1.0] - 2026-05-02

### ✨ Neue Features
- **T.xml Datei-Support**: 
  - Automatisches Auslesen von T.xml beim Öffnen eines Projektordners
  - Automatisches Befüllen der Main-Class aus der Konfiguration
  - Dynamischer Fenster-Titel basierend auf appName und Version
  - Konfigurierbare Projekteinstellungen

- **TBuild-Integration**: Erste Version mit TBuild.jar Support
- **Verbesserte Workflow-Optimierung**: Dependency-Management und Build-Prozess

### 🔧 Interne Verbesserungen
- **Build-Workflow**: GitHub Actions Konfiguration überarbeitet
- **Release-Prozess**: Vollständig automatisierte Release-Pipeline eingerichtet
- **Dokumentation**: Workflow-Kommentare verbessert

### 📝 Notizen
- TBuild.jar wird als neue Abhängigkeit integriert
- Fehlerhafte erste Release wurde gelöscht und durch diese Version ersetzt

---

## Legende

- ✨ **Neue Features** - Neue Funktionen hinzugefügt
- 🔧 **Bugfixes & Verbesserungen** - Fehlerbehebungen und Optimierungen
- 🎨 **UI/UX Verbesserungen** - Benutzeroberflächen-Änderungen
- 📦 **Abhängigkeiten & Build** - Änderungen an Dependencies oder Build-System
- 📝 **Dokumentation** - Änderungen an Dokumentation
- 🐛 **Bugfixes** - Spezifische Fehlerbehebungen
- 🔄 **Änderungen** - Allgemeine Änderungen

---

**Projekt**: TIDE - Thillagers leichte Java IDE  
**Repository**: [github.com/Thillager/TIDE](https://github.com/Thillager/TIDE)  
**Lizenz**: MIT
