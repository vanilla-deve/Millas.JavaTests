// Importing Java Swing libraries for GUI components
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

// Main class that represents the Game Launcher window
public class GameLauncher extends JFrame {

    // Model that holds a list of Game objects (used by JList)
    private DefaultListModel<Game> listModel = new DefaultListModel<>();

    // Visual list that displays games
    private JList<Game> gameList = new JList<>(listModel);

    // Text area that shows logs or messages (the console)
    private JTextArea console = new JTextArea();

    // Path to the file where the game list is stored locally
    private static final Path DATA_FILE = Paths.get("games.txt");

    // Main entry point of the program
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> { // ensures GUI runs on the Swing thread
            try {
                // Sets the look and feel to match the system (Windows, macOS, etc.)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            // Create and show the main window
            new GameLauncher().setVisible(true);
        });
    }

    // Constructor - sets up the window
    public GameLauncher() {
        super("GameSpace"); // Sets the window title
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600); // Window size
        setLocationRelativeTo(null); // Center on screen
        initUI(); // Build the user interface
        loadGames(); // Load saved games from file
    }

    // Method that creates all the interface components
    private void initUI() {

        // Left panel for the game list
        JPanel left = new JPanel(new BorderLayout(8,8));
        left.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        // Configure the game list
        gameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gameList.setCellRenderer(new GameCellRenderer()); // custom look for list items
        JScrollPane listScroll = new JScrollPane(gameList);
        left.add(listScroll, BorderLayout.CENTER);

        // Buttons under the game list
        JPanel leftButtons = new JPanel(new GridLayout(1,5,6,6));
        JButton addBtn = new JButton("Add");
        JButton editBtn = new JButton("Edit");
        JButton removeBtn = new JButton("Remove");
        JButton launchBtn = new JButton("Launch");
        JButton testBtn = new JButton("Test Path");

        // Add buttons to the panel
        leftButtons.add(addBtn);
        leftButtons.add(editBtn);
        leftButtons.add(removeBtn);
        leftButtons.add(launchBtn);
        leftButtons.add(testBtn);
        left.add(leftButtons, BorderLayout.SOUTH);

        // Add left side to the window
        add(left, BorderLayout.WEST);

        // ===== RIGHT SIDE =====
        JPanel right = new JPanel(new BorderLayout(8,8));
        right.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        // Info panel showing details about the selected game
        JPanel info = new JPanel(new BorderLayout(6,6));
        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        info.add(new JLabel("Selected Game Details:"), BorderLayout.NORTH);
        info.add(new JScrollPane(details), BorderLayout.CENTER);
        right.add(info, BorderLayout.CENTER);

        // Console area where log messages appear
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane consoleScroll = new JScrollPane(console);
        consoleScroll.setPreferredSize(new Dimension(400, 200));
        right.add(consoleScroll, BorderLayout.SOUTH);

        // Add right panel to window
        add(right, BorderLayout.CENTER);

        // When a game is selected in the list, show its details
        gameList.addListSelectionListener(ev -> {
            Game g = gameList.getSelectedValue();
            if (g == null) details.setText("");
            else details.setText(g.detailedString());
        });

        // ===== BUTTON FUNCTIONALITY =====

        // Add new game
        addBtn.addActionListener(e -> addOrEditGame(null));

        // Edit selected game
        editBtn.addActionListener(e -> {
            Game g = gameList.getSelectedValue();
            if (g == null) {
                JOptionPane.showMessageDialog(this, "Select a game to edit.", "No selection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            addOrEditGame(g);
        });

        // Remove selected game
        removeBtn.addActionListener(e -> {
            int idx = gameList.getSelectedIndex();
            if (idx >= 0) {
                if (JOptionPane.showConfirmDialog(this, "Remove selected game?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    listModel.remove(idx);
                    saveGames(); // Save list after deletion
                    consoleAppend("Removed game at index " + idx + "\n");
                }
            } else JOptionPane.showMessageDialog(this, "Select a game to remove.", "No selection", JOptionPane.INFORMATION_MESSAGE);
        });

        // Launch game (runs the executable)
        launchBtn.addActionListener(e -> {
            Game g = gameList.getSelectedValue();
            if (g == null) { JOptionPane.showMessageDialog(this, "Select a game to launch.", "No selection", JOptionPane.INFORMATION_MESSAGE); return; }
            launchGame(g);
        });

        // Test if the game path exists
        testBtn.addActionListener(e -> {
            Game g = gameList.getSelectedValue();
            if (g == null) { JOptionPane.showMessageDialog(this, "Select a game to test.", "No selection", JOptionPane.INFORMATION_MESSAGE); return; }
            boolean exists = Files.exists(Paths.get(g.path));
            JOptionPane.showMessageDialog(this, exists ? "Path exists." : "Path NOT found.", "Test Result", exists ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        });

        // ===== MENU BAR =====
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem importItem = new JMenuItem("Import...");
        JMenuItem exportItem = new JMenuItem("Export...");
        JMenuItem exitItem = new JMenuItem("Exit");

        // Add items to menu
        file.add(importItem);
        file.add(exportItem);
        file.addSeparator();
        file.add(exitItem);
        mb.add(file);
        setJMenuBar(mb);

        // Menu actions
        exitItem.addActionListener(e -> System.exit(0));
        importItem.addActionListener(e -> importFrom());
        exportItem.addActionListener(e -> exportTo());
    }

    // Opens the add/edit game dialog
    private void addOrEditGame(Game toEdit) {
        AddGameDialog dlg = new AddGameDialog(this, toEdit);
        dlg.setVisible(true);
        Game result = dlg.getResult();
        if (result != null) {
            if (toEdit != null) {
                int idx = listModel.indexOf(toEdit);
                listModel.set(idx, result);
                consoleAppend("Edited game: " + result.name + "\n");
            } else {
                listModel.addElement(result);
                consoleAppend("Added game: " + result.name + "\n");
            }
            saveGames(); // Save after add/edit
        }
    }

    // Method that runs (launches) the selected game
    private void launchGame(Game g) {
        Path p = Paths.get(g.path);
        if (!Files.exists(p)) {
            JOptionPane.showMessageDialog(this, "Executable not found: " + g.path, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        consoleAppend("Launching " + g.name + " -> " + g.path + " " + g.args + "\n");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(g.path);
            if (!g.args.isBlank()) {
                // Split arguments by spaces (basic version)
                for (String s : g.args.split(" ")) if (!s.isBlank()) cmd.add(s);
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(p.getParent().toFile());
            pb.redirectErrorStream(true);
            Process proc = pb.start(); // Start the process

            // Read the game’s output and print to console
            new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        consoleAppend(line + "\n");
                    }
                } catch (IOException ex) {
                    consoleAppend("Error reading process output: " + ex.getMessage() + "\n");
                }
            }, "proc-stdout-reader").start();

            // Wait for process to finish in another thread
            new Thread(() -> {
                try {
                    int exit = proc.waitFor();
                    consoleAppend("Process exited with code: " + exit + "\n");
                } catch (InterruptedException ex) {
                    consoleAppend("Process wait interrupted\n");
                }
            }, "proc-waiter").start();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to launch: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            consoleAppend("Launch failed: " + ex.getMessage() + "\n");
        }
    }

    // Helper method: adds text to the console safely from any thread
    private void consoleAppend(String text) {
        SwingUtilities.invokeLater(() -> {
            console.append(text);
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    // Saves the list of games to the text file
    private void saveGames() {
        try (BufferedWriter w = Files.newBufferedWriter(DATA_FILE, StandardCharsets.UTF_8)) {
            for (int i = 0; i < listModel.size(); i++) {
                Game g = listModel.get(i);
                // Each game stored as: name|path|args
                w.write(escape(g.name) + "|" + escape(g.path) + "|" + escape(g.args));
                w.newLine();
            }
        } catch (IOException ex) {
            consoleAppend("Failed to save games: " + ex.getMessage() + "\n");
        }
    }

    // Loads games from the saved file at startup
    private void loadGames() {
        if (!Files.exists(DATA_FILE)) return;
        try (BufferedReader r = Files.newBufferedReader(DATA_FILE, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] parts = splitLine(line);
                if (parts.length >= 3) {
                    Game g = new Game(unescape(parts[0]), unescape(parts[1]), unescape(parts[2]));
                    listModel.addElement(g);
                }
            }
        } catch (IOException ex) {
            consoleAppend("Failed to load games: " + ex.getMessage() + "\n");
        }
    }

    // Imports a game list from an external file
    private void importFrom() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path p = fc.getSelectedFile().toPath();
            try (BufferedReader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                listModel.clear();
                String line;
                while ((line = r.readLine()) != null) {
                    String[] parts = splitLine(line);
                    if (parts.length >= 3) listModel.addElement(new Game(unescape(parts[0]), unescape(parts[1]), unescape(parts[2])));
                }
                saveGames();
                consoleAppend("Imported games from " + p + "\n");
            } catch (IOException ex) { JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    // Exports the game list to a chosen file
    private void exportTo() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path p = fc.getSelectedFile().toPath();
            try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                for (int i = 0; i < listModel.size(); i++) {
                    Game g = listModel.get(i);
                    w.write(escape(g.name) + "|" + escape(g.path) + "|" + escape(g.args));
                    w.newLine();
                }
                consoleAppend("Exported games to " + p + "\n");
            } catch (IOException ex) { JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    // Escape special characters for saving text safely
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n");
    }

    // Revert escaped text to its original form
    private static String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\|", "|").replace("\\\\", "\\");
    }

    // Splits a saved line into name, path, and args safely (handles escaped |)
    private static String[] splitLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean esc = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (esc) { cur.append(c); esc = false; continue; }
            if (c == '\\') { esc = true; continue; }
            if (c == '|') { parts.add(cur.toString()); cur.setLength(0); continue; }
            cur.append(c);
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    // Custom list cell renderer for displaying games nicely in the list
    private static class GameCellRenderer extends JLabel implements ListCellRenderer<Game> {
        public GameCellRenderer() { setOpaque(true); setBorder(BorderFactory.createEmptyBorder(4,4,4,4)); }
        @Override
        public Component getListCellRendererComponent(JList<? extends Game> list, Game value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.name + "  (" + value.path + ")");
            setToolTipText(value.detailedString());
            setBackground(isSelected ? new Color(0xDDEEFF) : Color.WHITE);
            setForeground(Color.BLACK);
            return this;
        }
    }

    // Dialog that appears when adding or editing a game
    private static class AddGameDialog extends JDialog {
        private JTextField nameField = new JTextField();
        private JTextField pathField = new JTextField();
        private JTextField argsField = new JTextField();
        private Game result = null;

        AddGameDialog(JFrame owner, Game initial) {
            super(owner, true);
            setTitle(initial == null ? "Add Game" : "Edit Game");
            setSize(600,200);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(8,8));

            JPanel form = new JPanel(new GridLayout(3,1,6,6));
            form.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
            form.add(labeled("Name:", nameField));
            form.add(labeledWithBrowse("Executable path:", pathField));
            form.add(labeled("Arguments (optional):", argsField));
            add(form, BorderLayout.CENTER);

            // Buttons (OK / Cancel)
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton ok = new JButton("OK");
            JButton cancel = new JButton("Cancel");
            buttons.add(ok); buttons.add(cancel);
            add(buttons, BorderLayout.SOUTH);

            // Pre-fill fields if editing
            if (initial != null) { nameField.setText(initial.name); pathField.setText(initial.path); argsField.setText(initial.args); }

            // OK button saves input
            ok.addActionListener(e -> {
                String name = nameField.getText().trim();
                String path = pathField.getText().trim();
                String args = argsField.getText().trim();
                if (name.isEmpty() || path.isEmpty()) { JOptionPane.showMessageDialog(this, "Name and path are required.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
                result = new Game(name, path, args);
                setVisible(false);
            });

            // Cancel button closes dialog without saving
            cancel.addActionListener(e -> { result = null; setVisible(false); });
        }

        // Helper for labeled field rows
        private JPanel labeled(String label, JComponent comp) {
            JPanel p = new JPanel(new BorderLayout(6,6));
            p.add(new JLabel(label), BorderLayout.WEST);
            p.add(comp, BorderLayout.CENTER);
            return p;
        }

        // Helper for labeled field with a file browser button
        private JPanel labeledWithBrowse(String label, JTextField field) {
            JPanel p = new JPanel(new BorderLayout(6,6));
            p.add(new JLabel(label), BorderLayout.WEST);
            p.add(field, BorderLayout.CENTER);
            JButton b = new JButton("Browse");
            b.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) field.setText(fc.getSelectedFile().getAbsolutePath());
            });
            p.add(b, BorderLayout.EAST);
            return p;
        }

        // Returns the game created/edited by the dialog
        public Game getResult() { return result; }
    }

    // Simple class to represent a Game with name, path, and optional arguments
    private static class Game {
        String name;
        String path;
        String args;
        Game(String name, String path, String args) { this.name = name; this.path = path; this.args = args == null ? "" : args; }
        @Override public String toString() { return name; }
        String detailedString() { return "Name: " + name + "\nPath: " + path + (args.isBlank() ? "" : "\nArgs: " + args); }
    }
}
