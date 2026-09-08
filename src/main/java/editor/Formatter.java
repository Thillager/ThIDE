package editor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import java.io.File;

/**
 * Formatiermodul für Quellcode.
 * Berücksichtigt Kommentare (// und /* *\/) sowie String-Literale bei der Klammerzählung.
 */
public class Formatter {

    public static void format(RSyntaxTextArea textArea, File activeFile) {
        if (textArea == null) return;

        // Cursor-Position (Zeile & Spalte) sichern
        int savedLine = 0;
        int savedCol = 0;
        try {
            int caretOffset = textArea.getCaretPosition();
            savedLine = textArea.getLineOfOffset(caretOffset);
            savedCol = caretOffset - textArea.getLineStartOffset(savedLine);
        } catch (Exception ignored) {}

        String fileName = activeFile != null ? activeFile.getName().toLowerCase() : "";
        boolean isPython = fileName.endsWith(".py");
        boolean isJavaLike = fileName.endsWith(".java")
                || fileName.endsWith(".c")
                || fileName.endsWith(".cpp")
                || fileName.endsWith(".h")
                || fileName.endsWith(".hpp");

        int tabSize = textArea.getTabSize();
        boolean useTabs = !textArea.getTabsEmulated();
        String text = textArea.getText();

        String formatted;
        if (isPython) {
            formatted = formatPython(text, tabSize);
        } else if (isJavaLike) {
            formatted = formatJavaLike(text, tabSize, useTabs);
        } else {
            formatted = stripTrailingWhitespace(text);
        }

        textArea.setText(formatted);

        // Cursor wiederherstellen
        try {
            int totalLines = textArea.getLineCount();
            int targetLine = Math.min(savedLine, totalLines - 1);
            int lineStart = textArea.getLineStartOffset(targetLine);
            int lineLen = textArea.getLineEndOffset(targetLine) - lineStart;
            int targetCol = Math.min(savedCol, Math.max(0, lineLen - 1));
            textArea.setCaretPosition(lineStart + targetCol);
        } catch (Exception ignored) {
            textArea.setCaretPosition(0);
        }
    }

    private static String formatJavaLike(String code, int tabSize, boolean useTabs) {
        String indentUnit = useTabs ? "\t" : " ".repeat(Math.max(1, tabSize));
        String[] lines = code.split("\n", -1);
        StringBuilder result = new StringBuilder(code.length() + 256);

        int indentLevel = 0;
        boolean inBlockComment = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = trimRight(line).stripLeading();

            if (trimmed.isEmpty()) {
                if (i < lines.length - 1) result.append("\n");
                continue;
            }

            boolean startsInBlockComment = inBlockComment;
            LineAnalysis analysis = analyzeJavaLine(line, inBlockComment);
            inBlockComment = analysis.endsInBlockComment;

            int openBrackets = analysis.openBrackets;
            int closeBrackets = analysis.closeBrackets;
            boolean startsWithClosing = analysis.startsWithClosing;

            if (startsWithClosing && !startsInBlockComment) {
                indentLevel = Math.max(0, indentLevel - 1);
                closeBrackets = Math.max(0, closeBrackets - 1);
            }

            int currentIndent = Math.max(0, indentLevel);
            result.append(indentUnit.repeat(currentIndent)).append(trimmed);

            if (i < lines.length - 1) result.append("\n");

            if (!startsInBlockComment) {
                indentLevel = Math.max(0, indentLevel + (openBrackets - closeBrackets));
            }
        }

        return result.toString();
    }

    private static LineAnalysis analyzeJavaLine(String line, boolean startInBlockComment) {
        boolean inBlockComment = startInBlockComment;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;

        int open = 0;
        int close = 0;

        String trimmedLeading = line.stripLeading();
        boolean startsWithClosing = !startInBlockComment && 
                (trimmedLeading.startsWith("}") || trimmedLeading.startsWith(")") || trimmedLeading.startsWith("]"));

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            char next = (i + 1 < line.length()) ? line.charAt(i + 1) : '\0';

            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (inChar) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '\'') {
                    inChar = false;
                }
                continue;
            }

            // Einzeilige Kommentare ignorieren
            if (c == '/' && next == '/') {
                break;
            }

            // Mehrzeilige Kommentare starten
            if (c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            // Strings & Literale ignorieren
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '\'') {
                inChar = true;
                continue;
            }

            // Klammern nur außerhalb von Strings & Kommentaren zählen
            if (c == '{' || c == '(' || c == '[') {
                open++;
            } else if (c == '}' || c == ')' || c == ']') {
                close++;
            }
        }

        return new LineAnalysis(open, close, startsWithClosing, inBlockComment);
    }

    private static class LineAnalysis {
        final int openBrackets;
        final int closeBrackets;
        final boolean startsWithClosing;
        final boolean endsInBlockComment;

        LineAnalysis(int open, int close, boolean startsWithClosing, boolean endsInBlockComment) {
            this.openBrackets = open;
            this.closeBrackets = close;
            this.startsWithClosing = startsWithClosing;
            this.endsInBlockComment = endsInBlockComment;
        }
    }

    private static String formatPython(String code, int tabSize) {
        String[] lines = code.split("\n", -1);
        StringBuilder result = new StringBuilder(code.length() + 64);

        for (int i = 0; i < lines.length; i++) {
            String trimmedRight = trimRight(lines[i]);
            String normalized = expandLeadingTabs(trimmedRight, tabSize);
            result.append(normalized);
            if (i < lines.length - 1) result.append("\n");
        }

        return result.toString();
    }

    private static String trimRight(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }

    private static String expandLeadingTabs(String line, int tabSize) {
        if (line.isEmpty() || line.charAt(0) == ' ') return line;
        StringBuilder sb = new StringBuilder(line.length() + 16);
        int col = 0;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '\t') {
                int spaces = tabSize - (col % tabSize);
                sb.append(" ".repeat(spaces));
                col += spaces;
                i++;
            } else if (c == ' ') {
                sb.append(' ');
                col++;
                i++;
            } else {
                sb.append(line, i, line.length());
                break;
            }
        }
        return sb.toString();
    }

    private static String stripTrailingWhitespace(String code) {
        String[] lines = code.split("\n", -1);
        StringBuilder result = new StringBuilder(code.length());
        for (int i = 0; i < lines.length; i++) {
            result.append(trimRight(lines[i]));
            if (i < lines.length - 1) result.append("\n");
        }
        return result.toString();
    }
}