package ui;

import model.FileNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.function.Consumer;

public class FileTreePanel extends JScrollPane {

    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private ConsolePanel consolePanel;
    private JFrame parent;

    private File clipboard = null;

    /** Callback: Datei wurde doppelt angeklickt */
    private Consumer<File> onFileOpen;

    public FileTreePanel(JFrame parent, ConsolePanel consolePanel, Consumer<File> onFileOpen) {
        this.parent       = parent;
        this.consolePanel = consolePanel;
        this.onFileOpen   = onFileOpen;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Kein Projekt geöffnet");
        treeModel = new DefaultTreeModel(root);
        fileTree  = new JTree(treeModel);
        fileTree.setBackground(new Color(30, 31, 34));
        fileTree.setBorder(new EmptyBorder(10, 10, 10, 10));

        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();
                    if (node != null && node.getUserObject() instanceof FileNode) {
                        File selectedFile = ((FileNode) node.getUserObject()).getFile();
                        if (selectedFile.isFile()) onFileOpen.accept(selectedFile);
                    }
                }
            }

            @Override public void mousePressed(MouseEvent me)  { handleTreePopup(me); }
            @Override public void mouseReleased(MouseEvent me) { handleTreePopup(me); }

            private void handleTreePopup(MouseEvent me) {
                if (me.isPopupTrigger()) showFileTreePopup(me, null);
            }
        });

        setViewportView(fileTree);
        setPreferredSize(new Dimension(250, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));
    }

    public void updateFileTree(File rootFolder) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new FileNode(rootFolder));
        buildTree(rootFolder, rootNode);
        treeModel.setRoot(rootNode);
    }

    private void buildTree(File folder, DefaultMutableTreeNode node) {
        File[] files = folder.listFiles();
        if (files == null) return;
        Arrays.sort(files, (a, b) -> a.isDirectory() == b.isDirectory()
                ? a.getName().compareToIgnoreCase(b.getName())
                : a.isDirectory() ? -1 : 1);
        for (File file : files) {
            if (file.getName().startsWith(".") || file.getName().equals("out")) continue;
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
            node.add(childNode);
            if (file.isDirectory()) buildTree(file, childNode);
        }
    }

    private void showFileTreePopup(MouseEvent me, File currentProjectFolder) {
        // currentProjectFolder wird per Callback aus MainWindow geholt
        showFileTreePopupInternal(me);
    }

    /** Öffentliche Methode – wird aus MainWindow mit dem aktuellen Projektordner aufgerufen */
    public void showFileTreePopup(MouseEvent me, File currentProjectFolder, Runnable refreshCallback) {
        JPopupMenu popup = new JPopupMenu();
        TreePath path = fileTree.getPathForLocation(me.getX(), me.getY());

        JMenuItem neuDatei      = new JMenuItem("Neue Datei");
        JMenuItem neuOrdner     = new JMenuItem("Neuer Ordner");
        JMenuItem umbenennen    = new JMenuItem("Umbenennen");
        JMenuItem delete        = new JMenuItem("Löschen");
        JMenuItem explorer      = new JMenuItem("In Explorer öffnen");
        JMenuItem copy          = new JMenuItem("Kopieren");
        JMenuItem cut           = new JMenuItem("Ausschneiden");
        JMenuItem paste         = new JMenuItem("Einfügen");
        JMenuItem aktualisieren = new JMenuItem("Aktualisieren");
        aktualisieren.addActionListener(e -> {
            if (currentProjectFolder != null) updateFileTree(currentProjectFolder);
        });

        if (path != null) {
            fileTree.setSelectionPath(path);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            File clickedFile = null;
            if (node.getUserObject() instanceof FileNode) {
                clickedFile = ((FileNode) node.getUserObject()).getFile();
            }
            final File finalClickedFile = clickedFile;

            neuDatei.addActionListener(e    -> createNewFile(finalClickedFile, currentProjectFolder));
            neuOrdner.addActionListener(e   -> createNewFolder(finalClickedFile, currentProjectFolder));
            copy.addActionListener(e        -> copyFile(finalClickedFile));
            cut.addActionListener(e         -> cutFile(finalClickedFile));
            paste.addActionListener(e       -> pasteFile(finalClickedFile, currentProjectFolder));
            delete.addActionListener(e      -> deleteFile(finalClickedFile, currentProjectFolder));
            umbenennen.addActionListener(e  -> renameFile(finalClickedFile, currentProjectFolder));
            explorer.addActionListener(e    -> openExplorer(finalClickedFile));

            popup.add(neuDatei);
            popup.add(neuOrdner);
            popup.addSeparator();
            popup.add(umbenennen);
            popup.add(delete);
            popup.addSeparator();
            popup.add(explorer);
            popup.addSeparator();
            popup.add(copy);
            popup.add(cut);
            popup.add(paste);
            popup.addSeparator();
            popup.add(aktualisieren);
        } else {
            neuDatei.addActionListener(e  -> createNewFile(currentProjectFolder, currentProjectFolder));
            neuOrdner.addActionListener(e -> createNewFolder(currentProjectFolder, currentProjectFolder));
            popup.add(neuDatei);
            popup.add(neuOrdner);
            popup.addSeparator();
            popup.add(aktualisieren);
        }

        popup.show(fileTree, me.getX(), me.getY());
    }

    // Fallback (intern, ohne currentProjectFolder)
    private void showFileTreePopupInternal(MouseEvent me) {
        // Dieses Popup wird nur aufgerufen, wenn kein Projektordner geöffnet ist
        JPopupMenu popup = new JPopupMenu();
        JMenuItem hint = new JMenuItem("Bitte zuerst Ordner öffnen");
        hint.setEnabled(false);
        popup.add(hint);
        popup.show(fileTree, me.getX(), me.getY());
    }

    // ======= Dateioperationen =======

    public void createNewFile(File target, File currentProjectFolder) {
        if (target == null || currentProjectFolder == null) return;
        File zielOrdner = target.isDirectory() ? target : target.getParentFile();
        String name = (String) JOptionPane.showInputDialog(
                parent, "Wie soll die neue Datei heißen?",
                "Neue Datei", JOptionPane.PLAIN_MESSAGE, null, null, "");
        if (name != null && !name.trim().isEmpty()) {
            try {
                new File(zielOrdner, name.trim()).createNewFile();
                updateFileTree(currentProjectFolder);
            } catch (IOException ex) {
                consolePanel.log("[FEHLER] Datei konnte nicht erstellt werden.\n", Color.RED);
            }
        }
    }

    public void createNewFolder(File target, File currentProjectFolder) {
        if (target == null || currentProjectFolder == null) return;
        File zielOrdner = target.isDirectory() ? target : target.getParentFile();
        String name = (String) JOptionPane.showInputDialog(
                parent, "Wie soll der neue Ordner heißen?",
                "Neuer Ordner", JOptionPane.PLAIN_MESSAGE, null, null, "");
        if (name != null && !name.trim().isEmpty()) {
            new File(zielOrdner, name.trim()).mkdirs();
            updateFileTree(currentProjectFolder);
        }
    }

    private void copyFile(File file) {
        if (file != null) {
            clipboard = file;
            consolePanel.log("[INFO] Kopiert: " + file.getName() + "\n", Color.LIGHT_GRAY);
        }
    }

    private void cutFile(File file) {
        if (file != null) {
            clipboard = file;
            consolePanel.log("[INFO] Ausgeschnitten: " + file.getName() + "\n", Color.LIGHT_GRAY);
        }
    }

    private void pasteFile(File target, File currentProjectFolder) {
        if (clipboard == null || currentProjectFolder == null) return;
        File zielOrdner = target.isDirectory() ? target : target.getParentFile();
        try {
            Files.copy(clipboard.toPath(),
                    new File(zielOrdner, clipboard.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            updateFileTree(currentProjectFolder);
            consolePanel.log("[INFO] Eingefügt: " + clipboard.getName() + "\n", Color.GREEN);
        } catch (IOException ex) {
            consolePanel.log("[FEHLER] Einfügen fehlgeschlagen.\n", Color.RED);
        }
    }

    private void deleteFile(File file, File currentProjectFolder) {
        if (file == null) return;
        int confirm = JOptionPane.showConfirmDialog(
                parent, "'" + file.getName() + "' wirklich löschen?",
                "Löschen bestätigen", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Files.walk(file.toPath())
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(java.nio.file.Path::toFile)
                        .forEach(File::delete);
                updateFileTree(currentProjectFolder);
                consolePanel.log("[INFO] Gelöscht: " + file.getName() + "\n", Color.LIGHT_GRAY);
            } catch (IOException ex) {
                consolePanel.log("[FEHLER] Löschen fehlgeschlagen.\n", Color.RED);
            }
        }
    }

    private void renameFile(File file, File currentProjectFolder) {
        if (file == null) return;
        String name = (String) JOptionPane.showInputDialog(
                parent, "Neuer Name:", "Umbenennen",
                JOptionPane.PLAIN_MESSAGE, null, null, file.getName());
        if (name != null && !name.trim().isEmpty()) {
            file.renameTo(new File(file.getParentFile(), name.trim()));
            updateFileTree(currentProjectFolder);
        }
    }

    private void openExplorer(File file) {
        try {
            File ordner = file.isDirectory() ? file : file.getParentFile();
            Desktop.getDesktop().open(ordner);
        } catch (IOException ex) {
            consolePanel.log("[FEHLER] Explorer konnte nicht geöffnet werden.\n", Color.RED);
        }
    }
}
