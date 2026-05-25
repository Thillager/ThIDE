package hotSwap;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;


public class HotSwapWorker {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("ERR Aufruf: HotSwapWorker <outFolder> <debugPort>");
            System.exit(1);
        }

        File outFolder = new File(args[0]);
        int debugPort;
        try {
            debugPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("ERR Ungültiger Port: " + args[1]);
            System.exit(1);
            return;
        }

        Map<String, byte[]> classBytes = new HashMap<>();
        try {
            collectClassBytes(outFolder, "", classBytes);
        } catch (IOException e) {
            System.out.println("ERR Lesen der .class-Dateien: " + e.getMessage());
            System.exit(1);
        }

        if (classBytes.isEmpty()) {
            System.out.println("ERR Keine .class-Dateien in " + outFolder);
            System.exit(1);
        }

        redefineClasses(classBytes, debugPort);
    }

    @SuppressWarnings({"unchecked"})
    private static void redefineClasses(Map<String, byte[]> classBytes, int debugPort) {
        try {
            ClassLoader jdi = ClassLoader.getPlatformClassLoader();

            Class<?> argIface       = jdi.loadClass("com.sun.jdi.connect.Connector$Argument");
            Class<?> refTypeIface   = jdi.loadClass("com.sun.jdi.ReferenceType");
            Class<?> connectorIface = jdi.loadClass("com.sun.jdi.connect.AttachingConnector");
            Class<?> vmIface        = jdi.loadClass("com.sun.jdi.VirtualMachine");

            Method connName         = connectorIface.getMethod("name");
            Method connDefaultArgs  = connectorIface.getMethod("defaultArguments");
            Method connAttach       = connectorIface.getMethod("attach", Map.class);
            Method argSetValue      = argIface.getMethod("setValue", String.class);
            Method vmAllClasses     = vmIface.getMethod("allClasses");
            Method vmRedefine       = vmIface.getMethod("redefineClasses", Map.class);
            Method vmDispose        = vmIface.getMethod("dispose");
            Method rtName           = refTypeIface.getMethod("name");

            // Bootstrap.virtualMachineManager()
            Class<?> bootstrap = jdi.loadClass("com.sun.jdi.Bootstrap");
            Object vmm = bootstrap.getMethod("virtualMachineManager").invoke(null);

            // vmm.attachingConnectors() → List<AttachingConnector>
            List<?> connectors = (List<?>) vmm.getClass()
                    .getMethod("attachingConnectors").invoke(vmm);

            // SocketAttachingConnector suchen
            Object socketConn = null;
            for (Object c : connectors) {
                if (((String) connName.invoke(c)).contains("SocketAttach")) {
                    socketConn = c;
                    break;
                }
            }
            if (socketConn == null) {
                System.out.println("ERR Kein SocketAttach-Connector gefunden");
                return;
            }

            // defaultArguments() und host/port setzen
            Map<String, Object> params = (Map<String, Object>) connDefaultArgs.invoke(socketConn);
            for (Map.Entry<String, Object> e : params.entrySet()) {
                String key = e.getKey().toLowerCase();
                if (key.equals("host") || key.equals("hostname"))
                    argSetValue.invoke(e.getValue(), "127.0.0.1");
                if (key.equals("port") || key.equals("address"))
                    argSetValue.invoke(e.getValue(), String.valueOf(debugPort));
            }

            // attach → VirtualMachine
            Object vm = connAttach.invoke(socketConn, params);

            // allClasses() → List<ReferenceType>
            List<?> allTypes = (List<?>) vmAllClasses.invoke(vm);

            // redefMap aufbauen
            Map<Object, byte[]> redefMap = new HashMap<>();
            int matched = 0;
            for (Map.Entry<String, byte[]> entry : classBytes.entrySet()) {
                for (Object rt : allTypes) {
                    if (entry.getKey().equals(rtName.invoke(rt))) {
                        redefMap.put(rt, entry.getValue());
                        matched++;
                        break;
                    }
                }
            }

            if (redefMap.isEmpty()) {
                vmDispose.invoke(vm);
                System.out.println("WARN Keine passenden Klassen im Zielprozess – läuft der Debug-Prozess noch?");
                return;
            }

            vmRedefine.invoke(vm, redefMap);
            vmDispose.invoke(vm);

            System.out.println("OK " + matched + "/" + classBytes.size());

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.out.println("ERR " + cause.getMessage());
        }
    }

    private static void collectClassBytes(File dir, String pkg, Map<String, byte[]> result) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectClassBytes(f, pkg.isEmpty() ? f.getName() : pkg + "." + f.getName(), result);
            } else if (f.getName().endsWith(".class")) {
                String name = f.getName().replace(".class", "");
                result.put(pkg.isEmpty() ? name : pkg + "." + name, Files.readAllBytes(f.toPath()));
            }
        }
    }
}
