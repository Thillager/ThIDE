package hotSwap;

import ui.ConsolePanel;

import java.awt.Color;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

/**
 * HotSwapEngine – ersetzt laufende Klassen per JDI direkt aus TIDE heraus.
 *
 * JDI wird über Reflection geladen damit TIDE nicht direkt gegen com.sun.jdi
 * kompiliert werden muss (Modul-Zugriffsrechte variieren je nach JDK).
 *
 * Einschränkungen der JVM:
 *   - Keine neuen Felder oder Methoden hinzufügbar
 *   - Keine Änderung der Klassenhierarchie
 *   - Methodenkörper / Logik kann frei geändert werden
 */
public class HotSwapEngine {

    private final ConsolePanel consolePanel;
    private final File projectFolder;
    private final File outFolder;

    public HotSwapEngine(ConsolePanel consolePanel, File projectFolder) {
        this.consolePanel = consolePanel;
        this.projectFolder = projectFolder;
        this.outFolder = new File(projectFolder, "out");
    }

    /**
     * Kompiliert alle Java-Dateien neu und ersetzt die Klassen per JDI
     * im laufenden Debug-Prozess. Kein Neustart.
     */
    public boolean hotSwap(File sourceRoot, List<File> javaFiles, int debugPort) {
        consolePanel.log("[HOTSWAP] Kompiliere...\n", Color.CYAN);

        if (!recompile(sourceRoot, javaFiles)) {
            consolePanel.log("[HOTSWAP] Kompilierung fehlgeschlagen.\n", Color.RED);
            return false;
        }

        // .class-Dateien einlesen: vollständiger Klassenname -> Bytes
        Map<String, byte[]> classBytes = new HashMap<>();
        try {
            collectClassBytes(outFolder, "", classBytes);
        } catch (IOException e) {
            consolePanel.log("[HOTSWAP] Fehler beim Lesen der .class-Dateien: " + e.getMessage() + "\n", Color.RED);
            return false;
        }

        if (classBytes.isEmpty()) {
            consolePanel.log("[HOTSWAP] Keine .class-Dateien gefunden.\n", Color.RED);
            return false;
        }

        consolePanel.log("[HOTSWAP] Verbinde mit Prozess auf Port " + debugPort + "...\n", Color.CYAN);
        return redefineClasses(classBytes, debugPort);
    }

    // -------------------------------------------------------------------------
    // Schritt 1: Kompilieren
    // -------------------------------------------------------------------------

    private boolean recompile(File sourceRoot, List<File> javaFiles) {
        if (javaFiles.isEmpty()) return false;

        File sourcesFile = new File(projectFolder, ".sources.txt");
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(sourcesFile), java.nio.charset.StandardCharsets.UTF_8))) {
            for (File f : javaFiles) pw.println(f.getAbsolutePath());
        } catch (IOException e) {
            consolePanel.log("[HOTSWAP] .sources.txt Fehler: " + e.getMessage() + "\n", Color.RED);
            return false;
        }

        String sep = System.getProperty("path.separator");
        String cmd = "javac -encoding UTF-8 -g -cp \"out" + sep + "libs/*\" "
                   + "-d out "
                   + "-sourcepath \"" + sourceRoot.getAbsolutePath() + "\" "
                   + "\"@" + sourcesFile.getName() + "\"";
        try {
            ProcessBuilder pb = isWindows()
                    ? new ProcessBuilder("cmd.exe", "/c", cmd)
                    : new ProcessBuilder("bash", "-c", cmd);
            pb.directory(projectFolder);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null)
                    consolePanel.log(line + "\n", Color.YELLOW);
            }
            return p.waitFor() == 0;
        } catch (Exception e) {
            consolePanel.log("[HOTSWAP] javac Fehler: " + e.getMessage() + "\n", Color.RED);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Schritt 2: .class-Bytes einsammeln
    // -------------------------------------------------------------------------

    private void collectClassBytes(File dir, String pkg, Map<String, byte[]> result) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectClassBytes(f, pkg.isEmpty() ? f.getName() : pkg + "." + f.getName(), result);
            } else if (f.getName().endsWith(".class") && !f.getName().contains("$")) {
                // Innere Klassen (mit $) separat behandeln
                String name = f.getName().replace(".class", "");
                result.put(pkg.isEmpty() ? name : pkg + "." + name, Files.readAllBytes(f.toPath()));
            } else if (f.getName().endsWith(".class")) {
                // Innere Klassen auch einsammeln
                String name = f.getName().replace(".class", "");
                result.put(pkg.isEmpty() ? name : pkg + "." + name, Files.readAllBytes(f.toPath()));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Schritt 3: JDI redefineClasses – direkt über Reflection in TIDE
    // -------------------------------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean redefineClasses(Map<String, byte[]> classBytes, int debugPort) {
        try {
            // JDI ClassLoader aufbauen (tools.jar für Java 8, sonst JDK-intern)
            ClassLoader jdiLoader = buildJdiClassLoader();

            // Bootstrap.virtualMachineManager()
            Class<?> bootstrapClass = jdiLoader.loadClass("com.sun.jdi.Bootstrap");
            Method vmmMethod = bootstrapClass.getMethod("virtualMachineManager");
            Object vmm = vmmMethod.invoke(null);

            // attachingConnectors() – SocketAttach finden
            Method attachingConnectors = vmm.getClass().getMethod("attachingConnectors");
            List<?> connectors = (List<?>) attachingConnectors.invoke(vmm);

            Object socketConn = null;
            for (Object c : connectors) {
                Method nameMeth = c.getClass().getMethod("name");
                String name = (String) nameMeth.invoke(c);
                if (name.contains("SocketAttach")) { socketConn = c; break; }
            }
            if (socketConn == null) {
                consolePanel.log("[HOTSWAP] Kein SocketAttach-Connector gefunden.\n", Color.RED);
                return false;
            }

            // defaultArguments() – host und port setzen
            Method defaultArgs = socketConn.getClass().getMethod("defaultArguments");
            Map<String, Object> params = (Map<String, Object>) defaultArgs.invoke(socketConn);

            for (Map.Entry<String, Object> e : params.entrySet()) {
                String key = e.getKey().toLowerCase();
                Object arg = e.getValue();
                Method setValue = arg.getClass().getMethod("setValue", String.class);
                if (key.equals("host") || key.equals("hostname"))
                    setValue.invoke(arg, "127.0.0.1");
                if (key.equals("port") || key.equals("address"))
                    setValue.invoke(arg, String.valueOf(debugPort));
            }

            // attach(params) → VirtualMachine
            Method attachMethod = socketConn.getClass().getMethod("attach", Map.class);
            Object vm = attachMethod.invoke(socketConn, params);

            // allClasses() → List<ReferenceType>
            Method allClasses = vm.getClass().getMethod("allClasses");
            List<?> allTypes = (List<?>) allClasses.invoke(vm);

            // redefMap aufbauen: ReferenceType → byte[]
            Class<?> refTypeClass   = jdiLoader.loadClass("com.sun.jdi.ReferenceType");
            Map<Object, byte[]> redefMap = new HashMap<>();
            int matched = 0;

            for (Map.Entry<String, byte[]> entry : classBytes.entrySet()) {
                String targetName = entry.getKey();
                for (Object rt : allTypes) {
                    Method nameMeth = rt.getClass().getMethod("name");
                    String rtName = (String) nameMeth.invoke(rt);
                    if (rtName.equals(targetName)) {
                        redefMap.put(rt, entry.getValue());
                        matched++;
                        break;
                    }
                }
            }

            if (redefMap.isEmpty()) {
                consolePanel.log("[HOTSWAP] Keine passenden Klassen im Zielprozess gefunden.\n"
                               + "[HOTSWAP] Stelle sicher dass der Debug-Prozess noch läuft.\n", Color.ORANGE);
                // VM aufräumen
                vm.getClass().getMethod("dispose").invoke(vm);
                return false;
            }

            // redefineClasses(Map<? extends ReferenceType, byte[]>)
            Method redefine = vm.getClass().getMethod("redefineClasses", Map.class);
            redefine.invoke(vm, redefMap);

            vm.getClass().getMethod("dispose").invoke(vm);

            consolePanel.log("[HOTSWAP] ✓ " + matched + "/" + classBytes.size()
                           + " Klasse(n) live ersetzt – kein Neustart nötig.\n", Color.GREEN);
            return true;

        } catch (Exception e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            // UnsupportedOperationException = Klassenstruktur geändert (neue Methode/Feld)
            if (msg != null && msg.contains("UnsupportedOperation")) {
                consolePanel.log("[HOTSWAP] Nicht möglich: Klassenstruktur wurde geändert "
                               + "(neue Methode/Feld hinzugefügt).\n"
                               + "[HOTSWAP] Bitte Prozess neu starten.\n", Color.ORANGE);
            } else {
                consolePanel.log("[HOTSWAP] Fehler: " + msg + "\n", Color.RED);
            }
            return false;
        }
    }

    /**
     * Baut einen ClassLoader der JDI enthält.
     * Java 8:  tools.jar muss geladen werden
     * Java 9+: JDI ist in jdk.jdi eingebaut, Standard-ClassLoader reicht
     */
    private ClassLoader buildJdiClassLoader() throws IOException {
        File toolsJar = new File(System.getProperty("java.home"), "../lib/tools.jar");
        if (toolsJar.exists()) {
            // Java 8
            return new URLClassLoader(
                    new URL[]{toolsJar.getCanonicalFile().toURI().toURL()},
                    getClass().getClassLoader());
        }
        // Java 9+ – JDI ist über den Platform ClassLoader erreichbar
        // (das Modul jdk.jdi ist standardmäßig geladen wenn JDWP aktiv ist)
        ClassLoader platformLoader = ClassLoader.getPlatformClassLoader();
        try {
            platformLoader.loadClass("com.sun.jdi.Bootstrap");
            return platformLoader;
        } catch (ClassNotFoundException e) {
            // Letzter Versuch: System ClassLoader
            return ClassLoader.getSystemClassLoader();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
