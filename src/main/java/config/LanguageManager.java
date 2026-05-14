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
        texts.put("DE_folder_opened", "Ordner geöffnet: ");
        texts.put("DE_no_txml", "Keine T.xml im Projektordner");
        texts.put("DE_error_txml", "T.xml konnte nicht gelesen werden");
        
        // English
        texts.put("EN_open", "Open folder");
        texts.put("EN_save", "Save");
        texts.put("EN_clear", "Clear console");
        texts.put("EN_about", "About");
        texts.put("EN_mode", "Mode: ");
        texts.put("EN_main", "Main Class: ");
        texts.put("EN_folder_opened", "Folder opened: ");
        texts.put("EN_no_txml", "No T.xml in project folder");
        texts.put("EN_error_txml", "Failed to read T.xml");
    }
    
    public static void set(Language l) { lang = l; }
    public static Language get() { return lang; }
    
    public static String t(String key) {
        String k = lang.code.toUpperCase() + "_" + key;
        return texts.getOrDefault(k, key);
    }
}
