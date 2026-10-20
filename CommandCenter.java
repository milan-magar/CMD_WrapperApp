import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class CommandCenter extends JFrame {
    
    private final JLabel appTitle = new JLabel("Command Prompt Wrapper", SwingConstants.CENTER);
    private final JLabel dateLabel = new JLabel("", SwingConstants.CENTER);
    private final JTabbedPane mainTabs = new JTabbedPane();
    private final CmdPanel cmdPanel = new CmdPanel();
    private final ThisPCPanel thisPCPanel;
    private final NetworkChuckPanel networkChuckPanel;
    private final JTextField themeInput = new JTextField(10);
    
    private boolean isNepaliDate = false;
    private Color currentBg = Color.BLACK;
    private Color currentFg = Color.WHITE;
    private final Preferences prefs = Preferences.userNodeForPackage(CommandCenter.class);
    
    private final String CACHE_DIR = "C:\\Users\\NewAdmin\\Desktop\\Projects\\cache";
    private int logCounter = 0;
    private JButton saveLogBtn;

    public CommandCenter() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception ignored) {}

        setTitle("Command Prompt Wrapper");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        thisPCPanel = new ThisPCPanel(cmdPanel);
        networkChuckPanel = new NetworkChuckPanel(cmdPanel, mainTabs);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dateLabel.setToolTipText("Click to toggle Nepali/English date");
        dateLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isNepaliDate = !isNepaliDate;
                updateDate();
            }
        });
        
        topPanel.add(appTitle, BorderLayout.CENTER);
        topPanel.add(dateLabel, BorderLayout.EAST);
        
        mainTabs.addTab("CMD", cmdPanel);
        mainTabs.addTab("ThisPC", thisPCPanel);
        mainTabs.addTab("NetworkChuck", networkChuckPanel);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        themePanel.add(new JLabel("Theme (e.g., 02 = Black BG, Green Text):"));
        themePanel.add(themeInput);
        
        JButton applyThemeBtn = new JButton("Apply Theme");
        applyThemeBtn.addActionListener(e -> applyTheme(themeInput.getText().trim()));
        themePanel.add(applyThemeBtn);
        
        // Theme Help Button
        JButton helpBtn = new JButton("?");
        helpBtn.setToolTipText("Show Theme Color Codes");
        helpBtn.addActionListener(e -> {
            String help = """
                    Theme Color Codes:
                    
                    0: Black
                    1: White
                    2: Green
                    3: Blue
                    4: Red
                    5: Yellow
                    6: Cyan
                    7: Magenta
                    8: Dark Gray
                    9: Light Gray
                    
                    Format: XX
                    First digit = Background Color
                    Second digit = Text Color
                    
                    Example: 02 (Black Background, Green Text)
                    """;
            JOptionPane.showMessageDialog(this, help, "Theme Help", JOptionPane.INFORMATION_MESSAGE);
        });
        themePanel.add(helpBtn);
        
        JCheckBox adminToggle = new JCheckBox("Run as Administrator");
        adminToggle.setSelected(isAdmin());
        adminToggle.addActionListener(e -> {
            if (adminToggle.isSelected() && !isAdmin()) {
                elevate();
            } else if (!adminToggle.isSelected() && isAdmin()) {
                JOptionPane.showMessageDialog(this, "Cannot drop privileges without restarting normally.");
                adminToggle.setSelected(true);
            }
        });
        themePanel.add(adminToggle);
        
        saveLogBtn = new JButton("Save Log (" + logCounter + "/5)");
        saveLogBtn.addActionListener(e -> saveLogToFile());
        
        bottomPanel.add(themePanel, BorderLayout.WEST);
        bottomPanel.add(saveLogBtn, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        add(mainTabs, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        updateDate();
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> updateDate());
        timer.start();
        
        String savedTheme = prefs.get("theme", "01");
        themeInput.setText(savedTheme);
        applyTheme(savedTheme); 
    }
    
    private boolean isAdmin() {
        try {
            Process p = Runtime.getRuntime().exec("net session");
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void elevate() {
        try {
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String classPath = System.getProperty("java.class.path");
            String mainClass = "CommandCenter";
            
            String psCommand = "Start-Process -Verb RunAs -FilePath '" + javaBin + "' -ArgumentList '-cp', '\"" + classPath + "\"', '" + mainClass + "'";
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", psCommand);
            pb.start();
            System.exit(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to elevate: " + ex.getMessage());
        }
    }

    private void saveLogToFile() {
        try {
            Path cachePath = Paths.get(CACHE_DIR);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }
            
            logCounter = (logCounter % 5) + 1; 
            Path logFile = cachePath.resolve("log_" + logCounter + ".txt");
            
            Files.writeString(logFile, cmdPanel.getConsoleText(), StandardCharsets.UTF_8);
            
            saveLogBtn.setText("Save Log (" + logCounter + "/5)");
            JOptionPane.showMessageDialog(this, "Log successfully saved to:\n" + logFile.toString());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save log: " + ex.getMessage());
        }
    }

    private void updateDate() {
        LocalDate now = LocalDate.now();
        if (isNepaliDate) {
            String nepaliDate = NepaliDateConverter.convertToBS(now);
            dateLabel.setText(nepaliDate);
        } else {
            dateLabel.setText("English: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
    }
    
    private void applyTheme(String code) {
        if (code == null || code.length() < 2) return;
        try {
            int bgCode = Character.getNumericValue(code.charAt(0));
            int fgCode = Character.getNumericValue(code.charAt(1));
            
            Color bg = getColor(bgCode);
            Color fg = getColor(fgCode);
            
            if (bg != null && fg != null) {
                currentBg = bg;
                currentFg = fg;
                prefs.put("theme", code); 
                
                UIManager.put("Panel.background", bg);
                UIManager.put("Panel.foreground", fg);
                UIManager.put("OptionPane.background", bg);
                UIManager.put("OptionPane.messageForeground", fg);
                UIManager.put("Button.background", bg.brighter());
                UIManager.put("Button.foreground", fg);
                UIManager.put("Label.foreground", fg);
                UIManager.put("CheckBox.background", bg);
                UIManager.put("CheckBox.foreground", fg);
                UIManager.put("TextField.background", bg.darker());
                UIManager.put("TextField.foreground", fg);
                UIManager.put("TextField.caretForeground", fg);
                UIManager.put("TextArea.background", bg);
                UIManager.put("TextArea.foreground", fg);
                UIManager.put("ScrollPane.background", bg);
                UIManager.put("Viewport.background", bg);
                UIManager.put("TabbedPane.background", bg);
                UIManager.put("TabbedPane.foreground", fg);
                UIManager.put("TabbedPane.selected", bg.brighter());
                UIManager.put("TabbedPane.selectedForeground", fg);
                UIManager.put("TabbedPane.contentAreaColor", bg);
                
                SwingUtilities.updateComponentTreeUI(this);
                styleComponentTree(getContentPane(), bg, fg);
                
                cmdPanel.updateTheme(bg, fg);
                thisPCPanel.updateTheme(bg, fg);
                
                this.getContentPane().setBackground(bg);
                this.repaint();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid theme code. Use format: 02");
        }
    }
    
    private Color getColor(int code) {
        return switch (code) {
            case 0 -> Color.BLACK;
            case 1 -> Color.WHITE;
            case 2 -> Color.GREEN;
            case 3 -> Color.BLUE;
            case 4 -> Color.RED;
            case 5 -> Color.YELLOW;
            case 6 -> Color.CYAN;
            case 7 -> Color.MAGENTA;
            case 8 -> new Color(40, 44, 52); 
            case 9 -> Color.LIGHT_GRAY;
            default -> null;
        };
    }
    
    private void styleComponentTree(Component c, Color bg, Color fg) {
        if (c == null) return;
        
        if (c instanceof JButton) {
            JButton btn = (JButton) c;
            btn.setBackground(bg.brighter());
            btn.setForeground(fg);
            btn.setOpaque(true);
            btn.setContentAreaFilled(true);
            btn.setBorderPainted(true);
            btn.setUI(new BasicButtonUI()); 
        } else if (c instanceof JTextField || c instanceof JTextArea || c instanceof JTextPane) {
            c.setBackground(bg.darker());
            c.setForeground(fg);
            if (c instanceof JTextComponent) {
                ((JTextComponent) c).setCaretColor(fg);
            }
            ((JComponent) c).setOpaque(true);
        } else if (c instanceof JCheckBox) {
            c.setBackground(bg);
            c.setForeground(fg);
            ((JComponent) c).setOpaque(true);
        } else if (c instanceof JList || c instanceof JTable) {
            c.setBackground(bg);
            c.setForeground(fg);
            ((JComponent) c).setOpaque(true);
        } else if (c instanceof JTabbedPane) {
            c.setBackground(bg);
            c.setForeground(fg);
        } else if (c instanceof JScrollPane || c instanceof JViewport) {
            c.setBackground(bg);
            c.setForeground(fg);
        } else {
            c.setBackground(bg);
            c.setForeground(fg);
        }

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                styleComponentTree(child, bg, fg);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CommandCenter().setVisible(true));
    }
    
    // =========================================================================
    // CMD Panel
    // =========================================================================
    static class CmdPanel extends JPanel {
        private final JTextArea console = new JTextArea();
        private final JTextField input = new JTextField();
        private String currentPath = System.getProperty("user.home");
        private final JLabel promptLabel = new JLabel("> ");
        private Process shellProcess;
        private BufferedWriter shellWriter;
        
        public CmdPanel() {
            setLayout(new BorderLayout());
            console.setEditable(false);
            console.setFont(new Font("Consolas", Font.PLAIN, 14));
            console.setMargin(new Insets(10, 10, 10, 10));
            
            JScrollPane scroll = new JScrollPane(console);
            scroll.setBorder(null);
            
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            promptLabel.setFont(new Font("Consolas", Font.BOLD, 14));
            input.setFont(new Font("Consolas", Font.PLAIN, 14));
            input.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            inputPanel.add(promptLabel, BorderLayout.WEST);
            inputPanel.add(input, BorderLayout.CENTER);
            
            add(scroll, BorderLayout.CENTER);
            add(inputPanel, BorderLayout.SOUTH);
            
            input.addActionListener(e -> executeCommand(input.getText()));
            
            startShell();
        }
        
        public String getConsoleText() {
            return console.getText();
        }
        
        private void startShell() {
            try {
                ProcessBuilder pb = new ProcessBuilder("cmd.exe");
                pb.redirectErrorStream(true);
                shellProcess = pb.start();
                shellWriter = new BufferedWriter(new OutputStreamWriter(shellProcess.getOutputStream()));
                
                Thread readerThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(shellProcess.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // Strip ANSI escape codes to prevent garbled text in JTextArea
                            String cleanLine = line.replaceAll("\u001B\\[[;\\d]*[mK]", "");
                            SwingUtilities.invokeLater(() -> console.append(cleanLine + "\n"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                readerThread.setDaemon(true);
                readerThread.start();
                
                printPrompt();
            } catch (Exception e) {
                console.append("Failed to start shell: " + e.getMessage() + "\n");
            }
        }
        
        private void printPrompt() {
            console.append("\n" + currentPath + "> ");
        }
        
        public void executeCommand(String cmd) {
            if (cmd.trim().isEmpty()) return;
            if (cmd.trim().equals("cls")) {
                console.setText("");
                printPrompt();
                input.setText("");
                return;
            }
            
            console.append(cmd + "\n");
            input.setText("");
            
            try {
                shellWriter.write(cmd + "\n");
                shellWriter.flush();
            } catch (IOException e) {
                console.append("Error: " + e.getMessage() + "\n");
            }
            
            if (cmd.trim().toLowerCase().startsWith("cd ")) {
                new Thread(() -> {
                    try { Thread.sleep(200); } catch (Exception ignored) {}
                    SwingUtilities.invokeLater(() -> {
                        File f = new File(currentPath);
                        if (cmd.trim().equals("cd ..")) {
                            if (f.getParentFile() != null) currentPath = f.getParentFile().getAbsolutePath();
                        } else if (!cmd.trim().equals("cd")) {
                            String target = cmd.trim().substring(3).replace("\"", "");
                            File newDir = new File(f, target);
                            if (newDir.exists() && newDir.isDirectory()) {
                                currentPath = newDir.getAbsolutePath();
                            }
                        }
                    });
                }).start();
            }
        }
        
        public void setCurrentPath(String path) {
            this.currentPath = path;
            executeCommand("cd /d \"" + path + "\"");
        }
        
        public void updateTheme(Color bg, Color fg) {
            console.setBackground(bg);
            console.setForeground(fg);
            console.setCaretColor(fg);
            input.setBackground(bg.darker());
            input.setForeground(fg);
            input.setCaretColor(fg);
            promptLabel.setForeground(fg);
        }
    }

    // =========================================================================
    // ThisPC Panel
    // =========================================================================
    static class ThisPCPanel extends JPanel {
        private final CmdPanel cmdPanel;
        private final DefaultListModel<String> listModel = new DefaultListModel<>();
        private final JList<String> fileList = new JList<>(listModel);
        private String currentPath = System.getProperty("user.home");
        
        public ThisPCPanel(CmdPanel cmdPanel) {
            this.cmdPanel = cmdPanel;
            setLayout(new BorderLayout(5, 5));
            setBorder(new EmptyBorder(10, 10, 10, 10));
            
            JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton upBtn = new JButton("Up (cd ..)");
            JButton newFolderBtn = new JButton("New Folder (mkdir)");
            JButton newFileBtn = new JButton("New File (type nul)");
            
            topBar.add(upBtn);
            topBar.add(newFolderBtn);
            topBar.add(newFileBtn);
            
            fileList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            fileList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        String selected = fileList.getSelectedValue();
                        if (selected != null && !selected.equals("..")) {
                            File f = new File(currentPath, selected.replace(" [DIR]", ""));
                            if (f.isDirectory()) {
                                currentPath = f.getAbsolutePath();
                                refreshList();
                                cmdPanel.setCurrentPath(currentPath);
                            } else {
                                openFileEditor(f);
                            }
                        }
                    }
                }
            });
            
            upBtn.addActionListener(e -> {
                File f = new File(currentPath);
                if (f.getParentFile() != null) {
                    currentPath = f.getParentFile().getAbsolutePath();
                    refreshList();
                    cmdPanel.setCurrentPath(currentPath);
                }
            });
            
            newFolderBtn.addActionListener(e -> {
                String name = JOptionPane.showInputDialog(this, "Folder Name:");
                if (name != null && !name.trim().isEmpty()) {
                    cmdPanel.executeCommand("mkdir \"" + name + "\"");
                    new javax.swing.Timer(300, ev -> refreshList()).start();
                }
            });
            
            newFileBtn.addActionListener(e -> {
                String name = JOptionPane.showInputDialog(this, "File Name (e.g., data.txt):");
                if (name != null && !name.trim().isEmpty()) {
                    cmdPanel.executeCommand("type nul > \"" + name + "\"");
                    new javax.swing.Timer(300, ev -> refreshList()).start();
                }
            });
            
            add(topBar, BorderLayout.NORTH);
            add(new JScrollPane(fileList), BorderLayout.CENTER);
            
            refreshList();
        }
        
        private void refreshList() {
            listModel.clear();
            File dir = new File(currentPath);
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files, (a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                });
                for (File f : files) {
                    listModel.addElement(f.getName() + (f.isDirectory() ? " [DIR]" : ""));
                }
            }
        }
        
        private void openFileEditor(File file) {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit: " + file.getName(), true);
            dialog.setSize(600, 400);
            dialog.setLocationRelativeTo(this);
            
            JTextArea editor = new JTextArea();
            // Using Dialog font supports a much wider range of Unicode characters than Consolas
            editor.setFont(new Font("Dialog", Font.PLAIN, 14));
            try {
                editor.setText(Files.readString(file.toPath(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                editor.setText("Error reading file (possibly binary or not UTF-8).");
            }
            
            JButton saveBtn = new JButton("Save Changes");
            saveBtn.addActionListener(ev -> {
                try {
                    Files.writeString(file.toPath(), editor.getText(), StandardCharsets.UTF_8);
                    JOptionPane.showMessageDialog(dialog, "Saved successfully!");
                    refreshList();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(dialog, "Error saving file: " + ex.getMessage());
                }
            });
            
            dialog.add(new JScrollPane(editor), BorderLayout.CENTER);
            dialog.add(saveBtn, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }
        
        public void updateTheme(Color bg, Color fg) {
            fileList.setBackground(bg);
            fileList.setForeground(fg);
            fileList.setSelectionBackground(fg);
            fileList.setSelectionForeground(bg);
        }
    }

    // =========================================================================
    // NetworkChuck Panel
    // =========================================================================
    static class NetworkChuckPanel extends JPanel {
        private final JTabbedPane subTabs = new JTabbedPane();
        private final CmdPanel cmdPanel;
        private final JTabbedPane mainTabs;
        
        public NetworkChuckPanel(CmdPanel cmdPanel, JTabbedPane mainTabs) {
            this.cmdPanel = cmdPanel;
            this.mainTabs = mainTabs;
            
            setLayout(new BorderLayout());
            
            JLabel hint = new JLabel("Left Click: Prompts for input (if needed) | Right Click: Runs the NetworkChuck example", SwingConstants.CENTER);
            hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            hint.setBorder(new EmptyBorder(5, 5, 5, 5));
            
            subTabs.addTab("Basic", createBasicPanel());
            subTabs.addTab("Advanced", createAdvancedPanel());
            
            add(hint, BorderLayout.NORTH);
            add(subTabs, BorderLayout.CENTER);
        }
        
        private JPanel createBasicPanel() {
            JPanel panel = new JPanel(new GridLayout(0, 3, 10, 10));
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            addBasicButton(panel, "ipconfig", null, null);
            addBasicButton(panel, "ipconfig /all", null, null);
            addBasicButton(panel, "ipconfig /all | findstr DNS", null, null);
            addBasicButton(panel, "ipconfig /release", null, null);
            addBasicButton(panel, "ipconfig /renew", null, null);
            addBasicButton(panel, "ipconfig /displaydns", null, null);
            addBasicButton(panel, "ipconfig /displaydns | clip", null, null);
            addBasicButton(panel, "ipconfig /flushdns", null, null);
            addBasicButton(panel, "nslookup link", "nslookup <INPUT>", "TEXT");
            
            return panel;
        }
        
        private JPanel createAdvancedPanel() {
            JPanel panel = new JPanel(new GridLayout(0, 3, 10, 10));
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            // Security, Files & Hiding Data
            addAdvancedButton(panel, "attrib -h -s -r folder_name", "attrib -h -s -r <PATH>", "FOLDER", "attrib -h -s -r secret_folder");
            addAdvancedButton(panel, "copy /b image.jpg + secret_folder.zip new_secret_photo.jpg", "copy /b <PATH> + secret_folder.zip new_secret_photo.jpg", "FILE", "copy /b kitten.jpg + secrets.zip secure_photo.jpg");
            addAdvancedButton(panel, "cipher /e /s:", null, null, "cipher /e /s:.");
            addAdvancedButton(panel, "attrib +h +s +r folder_name", "attrib +h +s +r <PATH>", "FOLDER", "attrib +h +s +r secret_folder");

            // Wi-Fi & Network Discovery
            addAdvancedButton(panel, "netsh wlan show profiles", null, null, "netsh wlan show profiles");
            addAdvancedButton(panel, "netsh wlan show profile \"NetworkName\" key=clear", "netsh wlan show profile \"<INPUT>\" key=clear", "TEXT", "netsh wlan show profile \"Coffee_Guest\" key=clear");
            addAdvancedButton(panel, "netsh wlan show profiles | findstr /c:\"All User Profile\" > wifi_passwords.txt", null, null, "netsh wlan show profiles | findstr /c:\"All User Profile\" > wifi_passwords.txt");

            // System & Shortcuts
            addAdvancedButton(panel, "systeminfo", null, null, "systeminfo");
            addAdvancedButton(panel, "explorer .", null, null, "explorer .");
            addAdvancedButton(panel, "subst s: \"C:\\path\\to\\folder\"", "subst s: <PATH>", "FOLDER", "subst s: \"C:\\Users\\NewAdmin\\Desktop\\Projects\"");

            // Curl Utilities (Web APIs)
            addAdvancedButton(panel, "curl wttr.in/Dallas", "curl wttr.in/<INPUT>", "TEXT:Dallas", "curl wttr.in/Dallas");
            addAdvancedButton(panel, "curl -I -s https://networkchuck.com", "curl -I -s <INPUT>", "TEXT:https://networkchuck.com", "curl -I -s https://networkchuck.com");
            addAdvancedButton(panel, "curl checkip.amazonaws.com", "curl <INPUT>", "TEXT:checkip.amazonaws.com", "curl checkip.amazonaws.com");
            addAdvancedButton(panel, "curl qrcode.show/https://networkchuck.com", "curl qrcode.show/<INPUT>", "TEXT:https://networkchuck.com", "curl qrcode.show/https://networkchuck.com");
            addAdvancedButton(panel, "curl -s \"https://www.youtube.com/feeds/videos.xml?channel_id=...\"", "curl -s \"https://www.youtube.com/feeds/videos.xml?channel_id=<INPUT>\"", "TEXT:UC2eYFnH61tmytImy1mTYvhA", "curl -s \"https://www.youtube.com/feeds/videos.xml?channel_id=UC2eYFnH61tmytImy1mTYvhA\"");

            // Convenience & Fun
            addAdvancedButton(panel, "start https://networkchuck.com", "start <INPUT>", "TEXT:https://networkchuck.com", "start https://github.com");
            addAdvancedButton(panel, "doskey /history", null, null, "doskey /history");
            
            return panel;
        }
        
        private void addBasicButton(JPanel parent, String fullCmd, String leftCmd, String inputType) {
            JButton btn = new JButton("<html><center>" + fullCmd.replace("|", "|<br>").replace(" ", "<br>") + "</center></html>");
            btn.setFocusPainted(false);
            btn.setMargin(new Insets(5, 5, 5, 5));
            btn.setToolTipText("Left Click: " + (leftCmd != null ? "Prompt for input" : "Run command"));
            
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        handleLeftClick(leftCmd != null ? leftCmd : fullCmd, inputType);
                    }
                }
            });
            parent.add(btn);
        }

        private void addAdvancedButton(JPanel parent, String label, String leftCmd, String leftType, String rightCmd) {
            JButton btn = new JButton("<html><center>" + label.replace("|", "|<br>").replace(" ", "<br>") + "</center></html>");
            btn.setFocusPainted(false);
            btn.setMargin(new Insets(5, 5, 5, 5));
            btn.setToolTipText("<html>Left Click: " + (leftCmd != null ? "Prompt for input" : "Run command") + "<br>Right Click: " + (rightCmd != null ? "Run Example" : "Run command") + "</html>");
            
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        handleRightClick(rightCmd);
                    } else if (SwingUtilities.isLeftMouseButton(e)) {
                        handleLeftClick(leftCmd != null ? leftCmd : label, leftType);
                    }
                }
            });
            parent.add(btn);
        }

        private void handleLeftClick(String cmd, String inputType) {
            String defaultValue = "";
            if (inputType != null && inputType.startsWith("TEXT:")) {
                defaultValue = inputType.substring(5);
                inputType = "TEXT";
            }

            if ("FILE".equals(inputType) || "FOLDER".equals(inputType)) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select a " + (inputType.equals("FILE") ? "File" : "Folder"));
                if ("FOLDER".equals(inputType)) chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    String path = chooser.getSelectedFile().getAbsolutePath();
                    String finalCmd = cmd.replace("<PATH>", "\"" + path + "\"");
                    cmdPanel.executeCommand(finalCmd);
                    mainTabs.setSelectedIndex(0);
                }
            } else if ("TEXT".equals(inputType)) {
                JDialog dialog = new JDialog();
                dialog.setTitle("Input Required");
                dialog.setModal(true);
                dialog.setLayout(new BorderLayout(10, 10));
                dialog.setSize(450, 200);
                dialog.setLocationRelativeTo(null);

                JTextField field = new JTextField(defaultValue);
                field.setFont(new Font("Segoe UI", Font.PLAIN, 14));

                JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
                JButton ghBtn = new JButton("GitHub");
                JButton dsBtn = new JButton("DeepSeek");
                JButton ytBtn = new JButton("YouTube");

                ghBtn.addActionListener(ev -> field.setText("https://github.com/"));
                dsBtn.addActionListener(ev -> field.setText("https://chat.deepseek.com/"));
                ytBtn.addActionListener(ev -> field.setText("https://youtube.com/"));

                iconPanel.add(ghBtn); iconPanel.add(dsBtn); iconPanel.add(ytBtn);

                JButton okBtn = new JButton("OK");
                okBtn.addActionListener(ev -> {
                    String text = field.getText().trim();
                    if (!text.isEmpty()) {
                        String finalCmd = cmd.replace("<INPUT>", text);
                        cmdPanel.executeCommand(finalCmd);
                        mainTabs.setSelectedIndex(0);
                        dialog.dispose();
                    }
                });

                JPanel topPanel = new JPanel(new BorderLayout());
                topPanel.add(new JLabel("Enter value for: " + cmd), BorderLayout.NORTH);
                topPanel.add(field, BorderLayout.CENTER);

                dialog.add(topPanel, BorderLayout.CENTER);
                dialog.add(iconPanel, BorderLayout.NORTH);
                dialog.add(okBtn, BorderLayout.SOUTH);
                dialog.setVisible(true);
            } else {
                cmdPanel.executeCommand(cmd);
                mainTabs.setSelectedIndex(0);
            }
        }

        private void handleRightClick(String rightCmd) {
            if (rightCmd == null) return;
            
            if (rightCmd.startsWith("start http")) {
                String defaultLink = rightCmd.substring(6); 
                JDialog dialog = new JDialog();
                dialog.setTitle("Open Link");
                dialog.setModal(true);
                dialog.setLayout(new BorderLayout(10, 10));
                dialog.setSize(450, 200);
                dialog.setLocationRelativeTo(null);

                JTextField field = new JTextField(defaultLink);
                field.setFont(new Font("Segoe UI", Font.PLAIN, 14));

                JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
                JButton ghBtn = new JButton("GitHub");
                JButton dsBtn = new JButton("DeepSeek");
                JButton ytBtn = new JButton("YouTube");

                ghBtn.addActionListener(ev -> field.setText("https://github.com/"));
                dsBtn.addActionListener(ev -> field.setText("https://chat.deepseek.com/"));
                ytBtn.addActionListener(ev -> field.setText("https://youtube.com/"));

                iconPanel.add(ghBtn); iconPanel.add(dsBtn); iconPanel.add(ytBtn);

                JButton okBtn = new JButton("Open");
                okBtn.addActionListener(ev -> {
                    String link = field.getText().trim();
                    if (!link.isEmpty()) {
                        cmdPanel.executeCommand("start " + link);
                        mainTabs.setSelectedIndex(0);
                        dialog.dispose();
                    }
                });

                JPanel topPanel = new JPanel(new BorderLayout());
                topPanel.add(new JLabel("Edit Link:"), BorderLayout.NORTH);
                topPanel.add(field, BorderLayout.CENTER);

                dialog.add(topPanel, BorderLayout.CENTER);
                dialog.add(iconPanel, BorderLayout.NORTH);
                dialog.add(okBtn, BorderLayout.SOUTH);
                dialog.setVisible(true);
            } else {
                cmdPanel.executeCommand(rightCmd);
                mainTabs.setSelectedIndex(0);
            }
        }
    }
}

// =========================================================================
// Nepali Date Converter
// =========================================================================
class NepaliDateConverter {
    private static final int[][] BS_DATA = {
        {2080, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
        {2081, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
        {2082, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30},
        {2083, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
        {2084, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
        {2085, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31},
        {2086, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31},
        {2087, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
        {2088, 31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30},
        {2089, 31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30},
        {2090, 31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31}
    };

    private static final LocalDate REF_AD_DATE = LocalDate.of(2023, 4, 14);
    private static final int REF_BS_YEAR = 2080;

    public static String convertToBS(LocalDate adDate) {
        if (adDate.isBefore(REF_AD_DATE)) {
            return "Date before supported range (2023 AD)";
        }

        long daysDifference = ChronoUnit.DAYS.between(REF_AD_DATE, adDate);
        int bsYear = REF_BS_YEAR;
        int bsMonth = 1;
        int bsDay = 1;

        int rowIndex = 0; 
        for (int i = 0; i < BS_DATA.length; i++) {
            if (BS_DATA[i][0] == bsYear) {
                rowIndex = i;
                break;
            }
        }

        while (daysDifference > 0) {
            int daysInCurrentMonth = BS_DATA[rowIndex][bsMonth]; 
            
            if (daysDifference >= daysInCurrentMonth) {
                daysDifference -= daysInCurrentMonth;
                bsMonth++;
                if (bsMonth > 12) {
                    bsMonth = 1;
                    bsYear++;
                    rowIndex++;
                    if (rowIndex >= BS_DATA.length) {
                        return "Date out of supported range";
                    }
                }
            } else {
                bsDay += daysDifference;
                daysDifference = 0;
            }
        }

        return String.format("Nepali: %04d-%02d-%02d", bsYear, bsMonth, bsDay);
    }
}