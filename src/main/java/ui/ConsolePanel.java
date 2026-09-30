package ui;

import com.jediterm.terminal.Questioner;
import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import config.TIDEProperties;
import config.Theme;

@SuppressWarnings({"serial", "this-escape"})
public class ConsolePanel extends JPanel {

    static {
        // ConPty-Backend für Windows erzwingen (unterstützt TrueColor & ANSI-Escapes)
        System.setProperty("pty4j.preferred.win.backend", "conpty");
    }

    private JediTermWidget terminalWidget;
    private JTextField dummyTerminalInput = new JTextField();
    private PtyProcess ptyProcess;
    private Thread processMonitorThread;

    public ConsolePanel() {
        super(new BorderLayout());

        Theme t = MainWindow.THEME;

        Color bg = (t != null && t.background != null)
                ? t.background
                : UIManager.getColor("Panel.background");

        if (bg == null) {
            bg = new Color(43, 45, 48);
        }

        Color fg = (t != null && t.foreground != null)
                ? t.foreground
                : UIManager.getColor("Panel.foreground");

        if (fg == null) {
            fg = new Color(220, 220, 220);
        }

        setBackground(bg);

        final Color finalBg = bg;
        final Color finalFg = fg;

        Font terminalFont = getBestMonospacedFont(14.0f);

        DefaultSettingsProvider settings = new DefaultSettingsProvider() {

            @Override
            public Font getTerminalFont() {
                return terminalFont;
            }

            @Override
            public float getTerminalFontSize() {
                return 14.0f;
            }

            @Override
            public TextStyle getDefaultStyle() {
                return new TextStyle(
                        new TerminalColor(
                                finalFg.getRed(),
                                finalFg.getGreen(),
                                finalFg.getBlue()
                        ),
                        new TerminalColor(
                                finalBg.getRed(),
                                finalBg.getGreen(),
                                finalBg.getBlue()
                        )
                );
            }
        };

        terminalWidget = new JediTermWidget(settings);
        terminalWidget.setBackground(bg);

        setupContextMenu();
        startTerminalSession();

        add(terminalWidget, BorderLayout.CENTER);

        setPreferredSize(
                new Dimension(0, TIDEProperties.CONSOLE_HEIGHT)
        );
    }

    private Font getBestMonospacedFont(float size) {
        String[] preferredFonts = {
                "JetBrains Mono",
                "Cascadia Code",
                "Fira Code",
                "Consolas",
                "Monospaced"
        };

        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();

        String[] availableFonts =
                ge.getAvailableFontFamilyNames();

        for (String fontName : preferredFonts) {
            for (String available : availableFonts) {
                if (available.equalsIgnoreCase(fontName)) {
                    return new Font(
                            available,
                            Font.PLAIN,
                            (int) size
                    );
                }
            }
        }

        return new Font(
                "Monospaced",
                Font.PLAIN,
                (int) size
        );
    }

    private void setupContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem copyItem = new JMenuItem("Kopieren");
        copyItem.addActionListener(e -> copySelectedText());

        JMenuItem pasteItem = new JMenuItem("Einfügen");
        pasteItem.addActionListener(e -> pasteFromClipboard());

        JMenuItem clearItem = new JMenuItem("Konsole leeren");
        clearItem.addActionListener(e -> clear());

        popupMenu.add(copyItem);
        popupMenu.add(pasteItem);
        popupMenu.addSeparator();
        popupMenu.add(clearItem);

        terminalWidget.getTerminalPanel().addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (e.isPopupTrigger()) {
                            showMenu(e);
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (e.isPopupTrigger()) {
                            showMenu(e);
                        }
                    }

                    private void showMenu(MouseEvent e) {
                        popupMenu.show(
                                e.getComponent(),
                                e.getX(),
                                e.getY()
                        );
                    }
                }
        );
    }

    /**
     * Kopiert die aktuelle Terminal-Auswahl.
     *
     * JediTerm 3.76 besitzt bei TerminalPanel keine
     * getSelectedText()-Methode.
     *
     * Deshalb wird JediTerms normale Ctrl+C-Aktion
     * über das Terminal-Panel ausgelöst.
     */
    private void copySelectedText() {
        if (terminalWidget == null ||
                terminalWidget.getTerminalPanel() == null) {
            return;
        }

        Component terminalPanel =
                terminalWidget.getTerminalPanel();

        long now = System.currentTimeMillis();

        KeyEvent press = new KeyEvent(
                terminalPanel,
                KeyEvent.KEY_PRESSED,
                now,
                KeyEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_C,
                KeyEvent.CHAR_UNDEFINED
        );

        KeyEvent release = new KeyEvent(
                terminalPanel,
                KeyEvent.KEY_RELEASED,
                now,
                KeyEvent.CTRL_DOWN_MASK,
                KeyEvent.VK_C,
                KeyEvent.CHAR_UNDEFINED
        );

        terminalPanel.dispatchEvent(press);
        terminalPanel.dispatchEvent(release);
    }

    private void pasteFromClipboard() {
        try {
            String text =
                    (String) Toolkit.getDefaultToolkit()
                            .getSystemClipboard()
                            .getData(DataFlavor.stringFlavor);

            if (text != null &&
                    ptyProcess != null &&
                    ptyProcess.isAlive()) {

                ptyProcess.getOutputStream().write(
                        text.getBytes(StandardCharsets.UTF_8)
                );

                ptyProcess.getOutputStream().flush();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void startTerminalSession() {
        try {
            TtyConnector connector = createTtyConnector();

            terminalWidget.setTtyConnector(connector);
            terminalWidget.start();

            monitorProcess();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TtyConnector createTtyConnector() throws Exception {
        Map<String, String> env =
                new HashMap<>(System.getenv());

        env.put("TERM", "xterm-256color");
        env.put("COLORTERM", "truecolor");

        String[] command = getShellCommand();

        String userHome =
                System.getProperty("user.home");

        ptyProcess = new PtyProcessBuilder()
                .setCommand(command)
                .setEnvironment(env)
                .setDirectory(userHome)
                .setInitialColumns(120)
                .setInitialRows(30)
                .start();

        return new PtyProcessTtyConnector(
                ptyProcess,
                StandardCharsets.UTF_8
        );
    }

    private String[] getShellCommand() {
        String os =
                System.getProperty("os.name")
                        .toLowerCase();

        if (os.contains("win")) {
            return new String[]{
                    "cmd.exe",
                    "/k",
                    "chcp 65001 > nul && cls"
            };
        } else {
            String shell = System.getenv("SHELL");

            if (shell == null || shell.isEmpty()) {
                shell = new File("/bin/zsh").exists()
                        ? "/bin/zsh"
                        : "/bin/bash";
            }

            return new String[]{
                    shell,
                    "-l"
            };
        }
    }

    private void monitorProcess() {
        if (processMonitorThread != null &&
                processMonitorThread.isAlive()) {

            processMonitorThread.interrupt();
        }

        processMonitorThread = new Thread(() -> {
            try {
                if (ptyProcess != null) {
                    ptyProcess.waitFor();

                    Thread.sleep(300);

                    SwingUtilities.invokeLater(
                            this::startTerminalSession
                    );
                }

            } catch (InterruptedException ignored) {
            }
        });

        processMonitorThread.setDaemon(true);
        processMonitorThread.start();
    }

    /**
     * Schreibt Text über den PTY-OutputStream, damit ANSI-Escape-Sequenzen
     * korrekt durch den VT-Parser von JediTerm verarbeitet werden.
     *
     * WICHTIG: writeCharacters() darf hier NICHT verwendet werden, da diese
     * Methode den ANSI-Parser bypassed und ESC-Zeichen als '?' ausgibt.
     * Außerdem desynchronisiert writeCharacters() den Cursor-State zwischen
     * PTY und Renderer, was dazu führt dass der Cursor beim nächsten
     * Shell-Output an der falschen Position erscheint.
     */
    public void log(String msg, Color color) {
        if (ptyProcess == null || !ptyProcess.isAlive()) {
            return;
        }

        try {
            String output;

            if (color != null) {
                output = String.format(
                        "\u001B[38;2;%d;%d;%dm%s\u001B[0m",
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        msg
                );
            } else {
                output = msg;
            }

            ptyProcess.getOutputStream().write(
                    output.getBytes(StandardCharsets.UTF_8)
            );

            ptyProcess.getOutputStream().flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String msg) {
        log(msg, null);
    }

    public void clear() {
        if (terminalWidget != null &&
                terminalWidget.getTerminal() != null) {

            terminalWidget.getTerminal().clearScreen();
        }
    }

    public JTextField getTerminalInput() {
        return dummyTerminalInput;
    }

    public void close() {
        if (processMonitorThread != null) {
            processMonitorThread.interrupt();
        }

        if (terminalWidget != null) {
            terminalWidget.close();
        }

        if (ptyProcess != null &&
                ptyProcess.isAlive()) {

            ptyProcess.destroy();
        }
    }

    public JediTermWidget getTerminalWidget() {
        return terminalWidget;
    }
}

class PtyProcessTtyConnector implements TtyConnector {

    private final PtyProcess process;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Charset writeCharset;
    private final byte[] readBuf = new byte[4096];

    public PtyProcessTtyConnector(
            PtyProcess process,
            Charset charset) {

        this.process = process;

        // write(): UTF-8 bleibt korrekt (User-Input -> Shell)
        this.writeCharset =
                charset != null
                        ? charset
                        : StandardCharsets.UTF_8;

        this.inputStream =
                process.getInputStream();

        this.outputStream =
                process.getOutputStream();
    }

    @Override
    public boolean init(Questioner questioner) {
        return true;
    }

    /**
     * Liest Bytes direkt aus dem PTY-InputStream und castet jeden Byte
     * als (byte & 0xFF) in den char[]-Buffer — kein UTF-8-Decoding.
     *
     * Das ist notwendig weil Windows-Tools (winfetch, neofetch etc.) häufig
     * C1-Kontrollcodes im Bereich 0x80–0x9F ausgeben, insbesondere
     * 0x9B (CSI = ESC [) als kompakte 8-Bit-Variante.
     *
     * Ein InputStreamReader mit UTF-8 würde 0x9B als ungültigen UTF-8-Startbyte
     * behandeln und ihn durch '?' ersetzen, wodurch ANSI-Farbsequenzen als
     * Literal-Text erscheinen. Mit byte & 0xFF kommen alle Bytes 0x00–0xFF
     * unverändert bei JediTerms VT-Parser an, der sie korrekt interpretiert.
     */
    @Override
    public int read(
            char[] buf,
            int offset,
            int length) throws IOException {

        int toRead = Math.min(length, readBuf.length);
        int n = inputStream.read(readBuf, 0, toRead);

        if (n <= 0) {
            return n;
        }

        for (int i = 0; i < n; i++) {
            buf[offset + i] = (char) (readBuf[i] & 0xFF);
        }

        return n;
    }

    @Override
    public void write(byte[] bytes)
            throws IOException {

        outputStream.write(bytes);
        outputStream.flush();
    }

    @Override
    public void write(String string)
            throws IOException {

        write(string.getBytes(writeCharset));
    }

    @Override
    public boolean isConnected() {
        return process != null &&
                process.isAlive();
    }

    @Override
    public void resize(
            Dimension termSize,
            Dimension pixelSize) {

        if (process != null &&
                isConnected() &&
                termSize != null) {

            int cols = termSize.width;
            int rows = termSize.height;

            if (cols > 0 && rows > 0) {
                process.setWinSize(
                        new WinSize(cols, rows)
                );
            }
        }
    }

    @Override
    public String getName() {
        return "PtyProcess";
    }

    @Override
    public int waitFor()
            throws InterruptedException {

        return process.waitFor();
    }

    @Override
    public boolean ready()
            throws IOException {

        return inputStream.available() > 0;
    }

    @Override
    public void close() {
        if (process != null &&
                process.isAlive()) {

            process.destroy();
        }
    }
}