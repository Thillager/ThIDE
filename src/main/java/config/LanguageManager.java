package config;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    
    public enum Language {
        DE("de", "Deutsch"),
        EN("en", "English");
        
        public String code;
        public String name;
        Language(String c, String n) { code=c; name=n; }
    }
    
    private static Language lang = Language.DE;
    private static Map<String, String> texts = new HashMap<>();
    
    static {
        // German
        texts.put("DE_open", "Ordner öffnen");
        texts.put("DE_save", "Speichern");
        texts.put("DE_clear", "Konsole leeren");
        texts.put("DE_about", "Über");
        texts.put("DE_mode", "Modus: ");
        texts.put("DE_main", "Main-Class: ");
        texts.put("DE_folder.opened", "Ordner geöffnet: ");
        texts.put("DE_no.txml", "Keine T.xml im Projektordner");
        texts.put("DE_error.txml", "T.xml konnte nicht gelesen werden");
        texts.put("DE_umbenennen", "Umbenennen");
        texts.put("DE_neuDatei", "Neue Datei");
        texts.put("DE_neuOrdner", "Neuer Ordner");
        texts.put("DE_delete", "Löschen");
        texts.put("DE_explorer", "Explorer");
        texts.put("DE_copy", "Kopieren");
        texts.put("DE_cut", "Ausschneiden");
        texts.put("DE_paste", "Einfügen");
        texts.put("DE_aktualisieren", "Aktualisieren");
        texts.put("DE_file.name", "Wie soll die Datei heißen?");
        texts.put("DE_AboutDialog.slogan", "<html><center>Einfache, leichte IDE fuer Anfaenger.<br></center></html>");
        texts.put("DE_AboutDialog.headline", "TIDE - Leichte Java IDE");
        texts.put("DE_AboutDialog.header", "Über TIDE");
        texts.put("DE_searchUpdates", "Nach Updates suchen");
        texts.put("DE_close", "Schließen");
        texts.put("DE_oeffnen", "Oeffnen");
        
        // English
        texts.put("EN_open", "Open folder");
        texts.put("EN_save", "Save");
        texts.put("EN_clear", "Clear console");
        texts.put("EN_about", "About");
        texts.put("EN_mode", "Mode: ");
        texts.put("EN_main", "Main Class: ");
        texts.put("EN_folder.opened", "Folder opened: ");
        texts.put("EN_no.txml", "No T.xml in project folder");
        texts.put("EN_error.txml", "Failed to read T.xml");
        texts.put("EN_umbenennen", "Rename");
        texts.put("EN_neuDatei", "New file");
        texts.put("EN_neuOrdner", "New folder");
        texts.put("EN_delete", "Delete");
        texts.put("EN_explorer", "Explorer");
        texts.put("EN_copy", "Copy");
        texts.put("EN_cut", "Cut");
        texts.put("EN_paste", "Paste");
        texts.put("EN_aktualisieren", "Refresh");
        texts.put("EN_file.name", "What shall be the name of the file?");
        texts.put("EN_AboutDialog.slogan", "<html><center>Easy, lightweight IDE for beginners.<br></center></html>");
        texts.put("EN_AboutDialog.headline", "TIDE - Lightweight java IDE");
        texts.put("EN_AboutDialog.header", "About TIDE");
        texts.put("EN_searchUpdates", "Search for updates");
        texts.put("EN_close", "Close");
        texts.put("EN_open", "Open");
    }
    
    public static void set(Language l) { lang = l; }
    public static Language get() { return lang; }
    
    public static String t(String key) {
        String k = lang.code.toUpperCase() + "_" + key;
        return texts.getOrDefault(k, key);
    }
}
