package update;

import java.io.File;

public class AdminUtils {

    public static boolean restartJarAsAdmin() {
        try {
            String javaExe = System.getProperty("java.home")
                    + "\\bin\\java.exe";

            String jarPath = new File(
                    AdminUtils.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getPath();

            String command = String.format(
                    "Start-Process '%s' -ArgumentList '-jar \"%s\" --do-update' -Verb RunAs",
                    javaExe,
                    jarPath
            );

            new ProcessBuilder(
                    "powershell",
                    "-Command",
                    command
            ).start();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}