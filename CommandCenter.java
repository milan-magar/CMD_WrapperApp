import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
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
    
    private final JTabbedPane cmdTabs = new JTabbedPane();
    private final CmdPanel thisPcCmd;
    private final CmdPanel chuckCmd;
    
    private final ThisPCPanel thisPCPanel;
    private final NetworkChuckPanel networkChuckPanel;
    private final JTextField themeInput = new JTextField(8);
    
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
        setSize(1280, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        
        thisPcCmd = new CmdPanel();
        chuckCmd = new CmdPanel();
        
        cmdTabs.addTab("ThisPC Terminal", thisPcCmd);
        cmdTabs.setTabComponentAt(0, new CloseableTab(cmdTabs, "ThisPC Terminal", false));
        
        cmdTabs.addTab("NetworkChuck Terminal", chuckCmd);
        cmdTabs.setTabComponentAt(1, new CloseableTab(cmdTabs, "NetworkChuck Terminal", false));
        
        thisPCPanel = new ThisPCPanel(thisPcCmd, mainTabs, cmdTabs);
        networkChuckPanel = new NetworkChuckPanel(chuckCmd, mainTabs, cmdTabs);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
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
        
        JPanel cmdContainer = new JPanel(new BorderLayout());
        JPanel cmdToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        cmdToolbar.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        JButton newTabBtn = new JButton("+ New Terminal");
        newTabBtn.addActionListener(e -> addNewCmdTab());
        cmdToolbar.add(newTabBtn);
        
        cmdContainer.add(cmdToolbar, BorderLayout.NORTH);
        cmdContainer.add(cmdTabs, BorderLayout.CENTER);
        
        mainTabs.addTab("CMD", cmdContainer);
        mainTabs.addTab("ThisPC", thisPCPanel);
        mainTabs.addTab("NetworkChuck", networkChuckPanel);
        
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        themePanel.add(new JLabel("Theme Code:"));
        themePanel.add(themeInput);
        
        JButton applyThemeBtn = new JButton("Apply");
        applyThemeBtn.addActionListener(e -> applyTheme(themeInput.getText().trim()));
        themePanel.add(applyThemeBtn);
        
        JButton helpBtn = new JButton("?");
        helpBtn.setToolTipText("Show Theme Color Codes");
        helpBtn.addActionListener(e -> showThemeHelp());
        themePanel.add(helpBtn);
        
        JCheckBox adminToggle = new JCheckBox("Administrator Mode");
        adminToggle.setSelected(isAdmin());
        adminToggle.addActionListener(e -> {
            if (adminToggle.isSelected() && !isAdmin()) {
                elevate();
            } else if (!adminToggle.isSelected() && isAdmin()) {
                JOptionPane.showMessageDialog(this, "Restart the app normally to drop privileges.");
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
    
    private void addNewCmdTab() {
        CmdPanel panel = new CmdPanel();
        int count = cmdTabs.getTabCount();
        String title = "Terminal " + (count + 1);
        cmdTabs.addTab(title, panel);
        cmdTabs.setTabComponentAt(count, new CloseableTab(cmdTabs, title, true));
        cmdTabs.setSelectedComponent(panel);
    }
    
    private void showThemeHelp() {
        String help = """
                Theme Color Codes:
                
                0: Black      5: Yellow
                1: White      6: Cyan
                2: Green      7: Magenta
                3: Blue       8: Dark Gray
                4: Red        9: Light Gray
                
                Format: XX
                First digit = Background Color
                Second digit = Text Color
                
                Example: 02 (Black Background, Green Text)
                """;
        JOptionPane.showMessageDialog(this, help, "Theme Help", JOptionPane.INFORMATION_MESSAGE);
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
            new ProcessBuilder("powershell", "-Command", psCommand).start();
            System.exit(0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to elevate: " + ex.getMessage());
        }
    }

    private void saveLogToFile() {
        Component comp = cmdTabs.getSelectedComponent();
        if (!(comp instanceof CmdPanel)) return;
        
        try {
            Path cachePath = Paths.get(CACHE_DIR);
            if (!Files.exists(cachePath)) Files.createDirectories(cachePath);
            
            logCounter = (logCounter % 5) + 1; 
            Path logFile = cachePath.resolve("log_" + logCounter + ".txt");
            Files.writeString(logFile, ((CmdPanel) comp).getConsoleText(), StandardCharsets.UTF_8);
            
            saveLogBtn.setText("Save Log (" + logCounter + "/5)");
            JOptionPane.showMessageDialog(this, "Log saved to:\n" + logFile.toString());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save log: " + ex.getMessage());
        }
    }

    private void updateDate() {
        LocalDate now = LocalDate.now();
        if (isNepaliDate) {
            dateLabel.setText(NepaliDateConverter.convertToBS(now));
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
                
                for (int i = 0; i < cmdTabs.getTabCount(); i++) {
                    Component c = cmdTabs.getComponentAt(i);
                    if (c instanceof CmdPanel) ((CmdPanel) c).updateTheme(bg, fg);
                }
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
            btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(fg.darker(), 1),
                new EmptyBorder(5, 10, 5, 10)
            ));
        } else if (c instanceof JTextField || c instanceof JTextArea || c instanceof JTextPane) {
            c.setBackground(bg.darker());
            c.setForeground(fg);
            if (c instanceof JTextComponent) ((JTextComponent) c).setCaretColor(fg);
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
    
    static class CloseableTab extends JPanel {
        public CloseableTab(JTabbedPane pane, String title, boolean canClose) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);
            
            JLabel label = new JLabel(title);
            label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 5));
            add(label);
            
            if (canClose) {
                JButton closeBtn = new JButton("x");
                closeBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
                closeBtn.setMargin(new Insets(0, 4, 0, 4));
                closeBtn.setContentAreaFilled(false);
                closeBtn.setFocusPainted(false);
                closeBtn.setBorderPainted(false);
                closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                closeBtn.setToolTipText("Close Terminal");
                closeBtn.addActionListener(e -> {
                    int index = pane.indexOfTabComponent(CloseableTab.this);
                    if (index != -1) {
                        pane.remove(index);
                    }
                });
                add(closeBtn);
            }
        }
    }
    
    // =========================================================================
    // CMD Panel
    // =========================================================================
    static class CmdPanel extends JPanel {
        private final JTextPane console = new JTextPane();
        private final JTextField input = new JTextField();
        private String currentPath = "C:\\Users";
        private final JLabel promptLabel = new JLabel();
        private Process shellProcess;
        private BufferedWriter shellWriter;
        private int fontSize = 14;
        private Color currentBg = Color.BLACK;
        private Color currentFg = Color.WHITE;
        
        private static final String PROMPT_TOKEN = "~CMD_WRAPPER_PROMPT_9F8X~";
        private final String FONT_NAME = "Consolas"; 

        public CmdPanel() {
            setLayout(new BorderLayout());
            console.setEditable(false);
            console.setMargin(new Insets(10, 10, 10, 10));
            
            JScrollPane scroll = new JScrollPane(console);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            
            // Floating Action Button (FAB) for Clear Screen
            JButton clsFab = new JButton("CLS");
            clsFab.setFont(new Font("Segoe UI", Font.BOLD, 12));
            clsFab.setToolTipText("Clear Screen");
            clsFab.setFocusPainted(false);
            clsFab.setBackground(new Color(60, 60, 60));
            clsFab.setForeground(Color.WHITE);
            clsFab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
            ));
            clsFab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            clsFab.addActionListener(e -> executeCommand("cls"));
            
            JPanel fabWrapper = new JPanel(new BorderLayout());
            fabWrapper.setOpaque(false);
            fabWrapper.setBorder(new EmptyBorder(0, 0, 20, 25));
            JPanel fabHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            fabHolder.setOpaque(false);
            fabHolder.add(clsFab);
            fabWrapper.add(fabHolder, BorderLayout.SOUTH);
            
            JLayeredPane layeredPane = new JLayeredPane();
            layeredPane.setLayout(new OverlayLayout(layeredPane));
            
            JPanel scrollWrapper = new JPanel(new BorderLayout());
            scrollWrapper.add(scroll, BorderLayout.CENTER);
            
            layeredPane.add(scrollWrapper, Integer.valueOf(0));
            layeredPane.add(fabWrapper, Integer.valueOf(1));
            
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            promptLabel.setFont(new Font(FONT_NAME, Font.BOLD, fontSize));
            input.setFont(new Font(FONT_NAME, Font.PLAIN, fontSize));
            input.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            inputPanel.add(promptLabel, BorderLayout.WEST);
            inputPanel.add(input, BorderLayout.CENTER);
            
            add(layeredPane, BorderLayout.CENTER);
            add(inputPanel, BorderLayout.SOUTH);
            
            input.addActionListener(e -> executeCommand(input.getText()));
            
            updatePromptLabel();
            setupZoom(scroll, console, input);
            applyFontSettings();
            
            startShell();
        }
        
        private void appendText(String text) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Document doc = console.getDocument();
                    doc.insertString(doc.getLength(), text, null);
                    console.setCaretPosition(doc.getLength());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            });
        }
        
        private void applyFontSettings() {
            StyledDocument doc = console.getStyledDocument();
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setFontFamily(style, FONT_NAME);
            StyleConstants.setFontSize(style, fontSize);
            StyleConstants.setForeground(style, currentFg);
            
            // -0.2f line spacing ensures block characters connect perfectly for QR codes
            StyleConstants.setLineSpacing(style, -0.2f); 
            
            console.setParagraphAttributes(style, true);
            doc.setCharacterAttributes(0, doc.getLength(), style, false);
        }
        
        private void updatePromptLabel() {
            promptLabel.setText(currentPath + "> ");
        }
        
        private void setupZoom(JScrollPane scroll, JComponent... comps) {
            MouseWheelListener zoomWheel = e -> {
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0) fontSize = Math.min(fontSize + 2, 40);
                    else fontSize = Math.max(fontSize - 2, 8);
                    updateFontSize();
                    e.consume();
                }
            };
            scroll.addMouseWheelListener(zoomWheel);
            
            for (JComponent comp : comps) {
                InputMap im = comp.getInputMap(JComponent.WHEN_FOCUSED);
                ActionMap am = comp.getActionMap();
                
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "zoomIn");
                am.put("zoomIn", new AbstractAction() {
                    @Override public void actionPerformed(ActionEvent e) {
                        fontSize = Math.min(fontSize + 2, 40);
                        updateFontSize();
                    }
                });
                
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "zoomOut");
                am.put("zoomOut", new AbstractAction() {
                    @Override public void actionPerformed(ActionEvent e) {
                        fontSize = Math.max(fontSize - 2, 8);
                        updateFontSize();
                    }
                });
            }
        }
        
        private void updateFontSize() {
            applyFontSettings();
            input.setFont(new Font(FONT_NAME, Font.PLAIN, fontSize));
            promptLabel.setFont(new Font(FONT_NAME, Font.BOLD, fontSize));
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
                            if (line.startsWith(PROMPT_TOKEN)) {
                                continue;
                            }
                            String cleanLine = line.replaceAll("\u001B\\[[;\\d]*[mK]", "");
                            appendText(cleanLine + "\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                readerThread.setDaemon(true);
                readerThread.start();
                
                SwingUtilities.invokeLater(() -> {
                    try {
                        shellWriter.write("chcp 65001>nul\r\n");
                        shellWriter.write("prompt " + PROMPT_TOKEN + "\r\n");
                        shellWriter.write("cd /d " + currentPath + "\r\n");
                        shellWriter.write("cls\r\n");
                        shellWriter.flush();
                        
                        console.setText("");
                        applyFontSettings();
                    } catch (IOException ignored) {}
                });
                
            } catch (Exception e) {
                appendText("Failed to start shell: " + e.getMessage() + "\n");
            }
        }
        
        public void executeCommand(String cmd) {
            if (cmd.trim().isEmpty()) return;
            if (cmd.trim().equalsIgnoreCase("cls")) {
                SwingUtilities.invokeLater(() -> {
                    console.setText("");
                    applyFontSettings();
                });
                input.setText("");
                return;
            }
            
            appendText(currentPath + "> " + cmd + "\n");
            input.setText("");
            
            try {
                shellWriter.write(cmd + "\r\n");
                shellWriter.flush();
            } catch (IOException e) {
                appendText("Error: " + e.getMessage() + "\n");
            }
            
            if (cmd.trim().toLowerCase().startsWith("cd ")) {
                new Thread(() -> {
                    try { Thread.sleep(200); } catch (Exception ignored) {}
                    SwingUtilities.invokeLater(() -> {
                        File f = new File(currentPath);
                        if (cmd.trim().equalsIgnoreCase("cd ..")) {
                            if (f.getParentFile() != null) {
                                currentPath = f.getParentFile().getAbsolutePath();
                                updatePromptLabel();
                            }
                        } else if (!cmd.trim().equalsIgnoreCase("cd")) {
                            String target = cmd.trim().substring(3).replace("\"", "");
                            File newDir = new File(f, target);
                            if (newDir.exists() && newDir.isDirectory()) {
                                currentPath = newDir.getAbsolutePath();
                                updatePromptLabel();
                            }
                        }
                    });
                }).start();
            }
        }
        
        public void updateTheme(Color bg, Color fg) {
            currentBg = bg;
            currentFg = fg;
            console.setBackground(bg);
            console.setCaretColor(fg);
            input.setBackground(bg.darker());
            input.setForeground(fg);
            input.setCaretColor(fg);
            promptLabel.setForeground(fg);
            applyFontSettings();
        }
    }

    // =========================================================================
    // ThisPC Panel
    // =========================================================================
    static class ThisPCPanel extends JPanel {
        private final CmdPanel thisPcCmd;
        private final JTabbedPane mainTabs;
        private final JTabbedPane cmdTabs;
        private final DefaultListModel<String> listModel = new DefaultListModel<>();
        private final JList<String> fileList = new JList<>(listModel);
        private final JTextField pathField = new JTextField();
        private String currentPath;
        private final Preferences prefs = Preferences.userNodeForPackage(CommandCenter.class);
        
        public ThisPCPanel(CmdPanel thisPcCmd, JTabbedPane mainTabs, JTabbedPane cmdTabs) {
            this.thisPcCmd = thisPcCmd;
            this.mainTabs = mainTabs;
            this.cmdTabs = cmdTabs;
            
            // Load initial path from preferences or default to user home
            currentPath = prefs.get("initialFolder", System.getProperty("user.home"));
            File testDir = new File(currentPath);
            if (!testDir.exists() || !testDir.isDirectory()) {
                currentPath = System.getProperty("user.home");
            }
            
            setLayout(new BorderLayout(5, 5));
            setBorder(new EmptyBorder(10, 10, 10, 10));
            
            // Top Bar (Buttons)
            JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton upBtn = new JButton("Up (cd ..)");
            JButton newFolderBtn = new JButton("New Folder");
            JButton newFileBtn = new JButton("New File");
            JButton setStartBtn = new JButton("Set Startup Folder");
            
            topBar.add(upBtn);
            topBar.add(newFolderBtn);
            topBar.add(newFileBtn);
            topBar.add(setStartBtn);
            
            // Path Bar
            JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
            pathPanel.add(new JLabel("Location: "), BorderLayout.WEST);
            pathField.setEditable(false);
            pathField.setFont(new Font("Segoe UI", Font.BOLD, 14));
            pathField.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            pathPanel.add(pathField, BorderLayout.CENTER);
            
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.add(topBar, BorderLayout.NORTH);
            headerPanel.add(pathPanel, BorderLayout.SOUTH);
            
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
                                runCmd("cd /d \"" + currentPath + "\"");
                            } else {
                                showOpenOptions(f);
                            }
                        }
                    }
                }
            });
            
            JPopupMenu popup = new JPopupMenu();
            JMenuItem openNormalItem = new JMenuItem("Open (Normal)");
            JMenuItem openBrailleItem = new JMenuItem("Open (Braille)");
            JMenuItem renameItem = new JMenuItem("Rename");
            JMenuItem deleteItem = new JMenuItem("Delete");
            popup.add(openNormalItem);
            popup.add(openBrailleItem);
            popup.addSeparator();
            popup.add(renameItem);
            popup.add(deleteItem);
            fileList.setComponentPopupMenu(popup);
            
            fileList.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int row = fileList.locationToIndex(e.getPoint());
                    if (row >= 0) fileList.setSelectedIndex(row);
                }
            });
            
            openNormalItem.addActionListener(e -> {
                String selected = fileList.getSelectedValue();
                if (selected != null && !selected.endsWith(" [DIR]")) {
                    openFileEditor(new File(currentPath, selected), false);
                }
            });
            
            openBrailleItem.addActionListener(e -> {
                String selected = fileList.getSelectedValue();
                if (selected != null && !selected.endsWith(" [DIR]")) {
                    openFileEditor(new File(currentPath, selected), true);
                }
            });
            
            renameItem.addActionListener(e -> {
                String selected = fileList.getSelectedValue();
                if (selected != null) {
                    String oldName = selected.replace(" [DIR]", "");
                    String newName = (String) JOptionPane.showInputDialog(
                            SwingUtilities.getWindowAncestor(this),
                            "Enter new name for " + oldName + ":",
                            "Rename",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            null,
                            oldName
                    );
                    if (newName != null && !newName.trim().isEmpty() && !newName.equals(oldName)) {
                        File oldFile = new File(currentPath, oldName);
                        File newFile = new File(currentPath, newName);
                        try {
                            Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            refreshList();
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(this, "Rename failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
            
            deleteItem.addActionListener(e -> {
                String selected = fileList.getSelectedValue();
                if (selected != null) {
                    String fileName = selected.replace(" [DIR]", "");
                    boolean isDir = selected.endsWith(" [DIR]");
                    int confirm = JOptionPane.showConfirmDialog(this, "Delete " + fileName + " permanently?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        File target = new File(currentPath, fileName);
                        try {
                            if (isDir) {
                                Files.walk(target.toPath())
                                     .sorted(Comparator.reverseOrder())
                                     .map(Path::toFile)
                                     .forEach(File::delete);
                            } else {
                                Files.deleteIfExists(target.toPath());
                            }
                            refreshList();
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
            
            upBtn.addActionListener(e -> {
                File f = new File(currentPath);
                if (f.getParentFile() != null) {
                    currentPath = f.getParentFile().getAbsolutePath();
                    refreshList();
                    runCmd("cd /d \"" + currentPath + "\"");
                }
            });
            
            newFolderBtn.addActionListener(e -> {
                String name = JOptionPane.showInputDialog(this, "Folder Name:");
                if (name != null && !name.trim().isEmpty()) {
                    File target = new File(currentPath, name);
                    runCmd("mkdir \"" + target.getAbsolutePath() + "\"");
                    new javax.swing.Timer(500, ev -> refreshList()).start();
                }
            });
            
            newFileBtn.addActionListener(e -> {
                String name = JOptionPane.showInputDialog(this, "File Name:");
                if (name != null && !name.trim().isEmpty()) {
                    File target = new File(currentPath, name);
                    runCmd("type nul > \"" + target.getAbsolutePath() + "\"");
                    new javax.swing.Timer(500, ev -> refreshList()).start();
                }
            });
            
            setStartBtn.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser(currentPath);
                chooser.setDialogTitle("Select Startup Folder");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    String selectedPath = chooser.getSelectedFile().getAbsolutePath();
                    prefs.put("initialFolder", selectedPath);
                    JOptionPane.showMessageDialog(this, "Startup folder updated!\nThe app will open here next time.");
                }
            });
            
            add(headerPanel, BorderLayout.NORTH);
            add(new JScrollPane(fileList), BorderLayout.CENTER);
            
            refreshList();
        }
        
        private void runCmd(String cmd) {
            thisPcCmd.executeCommand(cmd);
        }
        
        private void refreshList() {
            pathField.setText(currentPath);
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
        
        private void showOpenOptions(File file) {
            Object[] options = {"Normal Mode", "Braille Mode", "Cancel"};
            int choice = JOptionPane.showOptionDialog(
                    this,
                    "How would you like to open " + file.getName() + "?",
                    "Open File",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            
            if (choice == 0) {
                openFileEditor(file, false);
            } else if (choice == 1) {
                openFileEditor(file, true);
            }
        }
        
        private void openFileEditor(File file, boolean isBraille) {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit: " + file.getName() + (isBraille ? " [Braille Mode]" : " [Normal Mode]"), true);
            dialog.setLayout(new BorderLayout());
            
            String content = "";
            try {
                content = Files.readString(file.toPath(), Charset.defaultCharset());
            } catch (IOException e) {
                content = "Error reading file (possibly binary).";
            }

            if (isBraille) {
                BrailleGridPanel gridPanel = new BrailleGridPanel(content);
                JScrollPane scroll = new JScrollPane(gridPanel);
                scroll.setBorder(null);
                scroll.getVerticalScrollBar().setUnitIncrement(16);
                scroll.getHorizontalScrollBar().setUnitIncrement(16);
                
                scroll.addMouseWheelListener(e -> {
                    if (e.isControlDown()) {
                        gridPanel.zoom(e.getWheelRotation() < 0);
                        e.consume();
                    }
                });
                
                dialog.add(scroll, BorderLayout.CENTER);
                
                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton closeBtn = new JButton("Close");
                closeBtn.addActionListener(e -> dialog.dispose());
                bottomPanel.add(closeBtn);
                dialog.add(bottomPanel, BorderLayout.SOUTH);
                
                dialog.pack();
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                int w = Math.min(dialog.getWidth(), screen.width - 100);
                int h = Math.min(dialog.getHeight(), screen.height - 100);
                dialog.setSize(w + 20, h + 60);
                
            } else {
                JTextArea editor = new JTextArea(content);
                int[] fontSize = {14};
                editor.setFont(new Font("Segoe UI Symbol", Font.PLAIN, fontSize[0]));
                editor.setLineWrap(true);
                editor.setWrapStyleWord(true);
                editor.setTabSize(4);
                editor.setMargin(new Insets(5, 5, 5, 5));
                editor.setBackground(Color.WHITE);
                editor.setForeground(Color.BLACK);
                editor.setCaretColor(Color.BLACK);
                
                JScrollPane scroll = new JScrollPane(editor);
                scroll.getVerticalScrollBar().setUnitIncrement(16);
                
                scroll.addMouseWheelListener(e -> {
                    if (e.isControlDown()) {
                        if (e.getWheelRotation() < 0) fontSize[0] = Math.min(fontSize[0] + 2, 40);
                        else fontSize[0] = Math.max(fontSize[0] - 2, 8);
                        editor.setFont(new Font("Segoe UI Symbol", Font.PLAIN, fontSize[0]));
                        e.consume();
                    }
                });
                
                InputMap im = editor.getInputMap(JComponent.WHEN_FOCUSED);
                ActionMap am = editor.getActionMap();
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
                am.put("zoomIn", new AbstractAction() {
                    @Override public void actionPerformed(ActionEvent e) {
                        fontSize[0] = Math.min(fontSize[0] + 2, 40);
                        editor.setFont(new Font("Segoe UI Symbol", Font.PLAIN, fontSize[0]));
                    }
                });
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
                am.put("zoomOut", new AbstractAction() {
                    @Override public void actionPerformed(ActionEvent e) {
                        fontSize[0] = Math.max(fontSize[0] - 2, 8);
                        editor.setFont(new Font("Segoe UI Symbol", Font.PLAIN, fontSize[0]));
                    }
                });
                
                dialog.add(scroll, BorderLayout.CENTER);

                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JCheckBox darkModeToggle = new JCheckBox("Dark Mode");
                darkModeToggle.addActionListener(e -> {
                    if (darkModeToggle.isSelected()) {
                        editor.setBackground(Color.BLACK);
                        editor.setForeground(Color.WHITE);
                        editor.setCaretColor(Color.WHITE);
                        scroll.getViewport().setBackground(Color.BLACK);
                    } else {
                        editor.setBackground(Color.WHITE);
                        editor.setForeground(Color.BLACK);
                        editor.setCaretColor(Color.BLACK);
                        scroll.getViewport().setBackground(Color.WHITE);
                    }
                });
                bottomPanel.add(darkModeToggle);

                JButton saveBtn = new JButton("Save Changes");
                saveBtn.addActionListener(ev -> {
                    try {
                        Files.writeString(file.toPath(), editor.getText(), Charset.defaultCharset());
                        JOptionPane.showMessageDialog(dialog, "Saved successfully!");
                        refreshList();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
                    }
                });
                bottomPanel.add(saveBtn);
                
                dialog.add(bottomPanel, BorderLayout.SOUTH);
                dialog.setSize(800, 600);
            }
            
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
        
        public void updateTheme(Color bg, Color fg) {
            fileList.setBackground(bg);
            fileList.setForeground(fg);
            fileList.setSelectionBackground(fg);
            fileList.setSelectionForeground(bg);
            pathField.setBackground(bg);
            pathField.setForeground(fg);
        }
    }

    // =========================================================================
    // NetworkChuck Panel
    // =========================================================================
    static class NetworkChuckPanel extends JPanel {
        private final JTabbedPane subTabs = new JTabbedPane();
        private final CmdPanel chuckCmd;
        private final JTabbedPane mainTabs;
        private final JTabbedPane cmdTabs;
        
        public NetworkChuckPanel(CmdPanel chuckCmd, JTabbedPane mainTabs, JTabbedPane cmdTabs) {
            this.chuckCmd = chuckCmd;
            this.mainTabs = mainTabs;
            this.cmdTabs = cmdTabs;
            
            setLayout(new BorderLayout());
            
            JLabel hint = new JLabel("Left Click: Prompt | Right Click: Run Example", SwingConstants.CENTER);
            hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            hint.setBorder(new EmptyBorder(5, 5, 5, 5));
            
            subTabs.addTab("Basic", createBasicPanel());
            subTabs.addTab("Advanced", createAdvancedPanel());
            
            add(hint, BorderLayout.NORTH);
            add(subTabs, BorderLayout.CENTER);
        }
        
        private void runCmd(String cmd) {
            chuckCmd.executeCommand(cmd);
            mainTabs.setSelectedIndex(0);
            cmdTabs.setSelectedIndex(1);
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
            addBasicButton(panel, "nslookup link", "nslookup <INPUT>", "TEXT:");
            
            return panel;
        }
        
        private JPanel createAdvancedPanel() {
            JPanel panel = new JPanel(new GridLayout(0, 3, 10, 10));
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            addAdvancedButton(panel, "attrib -h -s -r folder_name", "attrib -h -s -r <PATH>", "FOLDER", "attrib -h -s -r secret_folder");
            addAdvancedButton(panel, "copy /b image.jpg + secret_folder.zip new_secret_photo.jpg", "copy /b <PATH> + secret_folder.zip new_secret_photo.jpg", "FILE", "copy /b kitten.jpg + secrets.zip secure_photo.jpg");
            addAdvancedButton(panel, "cipher /e /s:", null, null, "cipher /e /s:.");
            addAdvancedButton(panel, "attrib +h +s +r folder_name", "attrib +h +s +r <PATH>", "FOLDER", "attrib +h +s +r secret_folder");

            addAdvancedButton(panel, "netsh wlan show profiles", null, null, "netsh wlan show profiles");
            addAdvancedButton(panel, "netsh wlan show profile \"NetworkName\" key=clear", "netsh wlan show profile \"<INPUT>\" key=clear", "TEXT:Coffee_Guest", "netsh wlan show profile \"Coffee_Guest\" key=clear");
            addAdvancedButton(panel, "netsh wlan show profiles | findstr /c:\"All User Profile\" > wifi_passwords.txt", null, null, "netsh wlan show profiles | findstr /c:\"All User Profile\" > wifi_passwords.txt");
            
            addAdvancedButton(panel, "Show All Wi-Fi Passwords", "SHOW_WIFI_PASSWORDS", null, "SHOW_WIFI_PASSWORDS");

            addAdvancedButton(panel, "systeminfo", null, null, "systeminfo");
            addAdvancedButton(panel, "explorer .", null, null, "explorer .");
            addAdvancedButton(panel, "subst s: \"C:\\path\\to\\folder\"", "subst s: <PATH>", "FOLDER", "subst s: \"C:\\Users\\NewAdmin\\Desktop\\Projects\"");

            addAdvancedButton(panel, "curl wttr.in/Dallas", "curl wttr.in/<INPUT>", "TEXT:Dallas", "curl wttr.in/Dallas");
            addAdvancedButton(panel, "curl -I -s https://networkchuck.com", "curl -I -s <INPUT>", "TEXT:https://networkchuck.com", "curl -I -s https://networkchuck.com");
            addAdvancedButton(panel, "curl checkip.amazonaws.com", "curl <INPUT>", "TEXT:checkip.amazonaws.com", "curl checkip.amazonaws.com");
            addAdvancedButton(panel, "curl qrcode.show/https://networkchuck.com", "curl qrcode.show/<INPUT>", "TEXT:https://networkchuck.com", "curl qrcode.show/https://networkchuck.com");
            
            // Updated Channel ID input type
            addAdvancedButton(panel, "curl -s \"https://www.youtube.com/feeds/videos.xml?channel_id=...\"", "curl -s \"https://www.youtube.com/feeds/videos.xml?channel_id=<INPUT>\"", "CHANNEL_ID:UC2eYFnH61tmytImy1mTYvhA", "curl -s \"https://www.youtube.com/feeds/videos.xml?channel_id=UC2eYFnH61tmytImy1mTYvhA\"");

            addAdvancedButton(panel, "start https://networkchuck.com", "start <INPUT>", "TEXT:https://networkchuck.com", "start https://github.com");
            addAdvancedButton(panel, "doskey /history", null, null, "doskey /history");
            
            return panel;
        }
        
        private void addBasicButton(JPanel parent, String fullCmd, String leftCmd, String inputType) {
            JButton btn = new JButton("<html><center>" + fullCmd.replace("|", "|<br>") + "</center></html>");
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
            JButton btn = new JButton("<html><center>" + label.replace("|", "|<br>") + "</center></html>");
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
            if ("SHOW_WIFI_PASSWORDS".equals(cmd)) {
                showWifiPasswords();
                return;
            }

            String defaultValue = "";
            boolean showUrlButtons = false;
            
            if (inputType != null) {
                if (inputType.startsWith("TEXT:")) {
                    defaultValue = inputType.substring(5);
                    inputType = "TEXT";
                    showUrlButtons = true;
                } else if (inputType.startsWith("CHANNEL_ID:")) {
                    defaultValue = inputType.substring(11);
                    inputType = "TEXT";
                    showUrlButtons = false; // Don't show URL buttons for Channel IDs
                }
            }

            if ("FILE".equals(inputType) || "FOLDER".equals(inputType)) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select a " + (inputType.equals("FILE") ? "File" : "Folder"));
                if ("FOLDER".equals(inputType)) chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                
                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    String path = chooser.getSelectedFile().getAbsolutePath();
                    runCmd(cmd.replace("<PATH>", "\"" + path + "\""));
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
                if (showUrlButtons) {
                    JButton ghBtn = new JButton("GitHub");
                    JButton dsBtn = new JButton("DeepSeek");
                    JButton ytBtn = new JButton("YouTube");

                    ghBtn.addActionListener(ev -> field.setText("https://github.com/"));
                    dsBtn.addActionListener(ev -> field.setText("https://chat.deepseek.com/"));
                    ytBtn.addActionListener(ev -> field.setText("https://youtube.com/"));

                    iconPanel.add(ghBtn); iconPanel.add(dsBtn); iconPanel.add(ytBtn);
                }

                JButton okBtn = new JButton("OK");
                okBtn.addActionListener(ev -> {
                    String text = field.getText().trim();
                    if (!text.isEmpty()) {
                        runCmd(cmd.replace("<INPUT>", text));
                        dialog.dispose();
                    }
                });

                JPanel topPanel = new JPanel(new BorderLayout());
                topPanel.add(new JLabel("Enter value for: " + cmd), BorderLayout.NORTH);
                topPanel.add(field, BorderLayout.CENTER);

                dialog.add(topPanel, BorderLayout.CENTER);
                if (showUrlButtons) {
                    dialog.add(iconPanel, BorderLayout.NORTH);
                }
                dialog.add(okBtn, BorderLayout.SOUTH);
                dialog.setVisible(true);
            } else {
                runCmd(cmd);
            }
        }

        private void handleRightClick(String rightCmd) {
            if (rightCmd == null) return;
            
            if ("SHOW_WIFI_PASSWORDS".equals(rightCmd)) {
                showWifiPasswords();
                return;
            }

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
                        runCmd("start " + link);
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
                runCmd(rightCmd);
            }
        }

        private void showWifiPasswords() {
            String script = "@echo off\n" +
                            "setlocal enabledelayedexpansion\n" +
                            "for /F \"tokens=2 delims=:\" %%a in ('netsh wlan show profile') do (\n" +
                            "    set wifi_pwd=\n" +
                            "    for /F \"tokens=2 delims=: usebackq\" %%F IN (`netsh wlan show profile %%a key^=clear ^| find \"Key Content\"`) do (\n" +
                            "        set wifi_pwd=%%F\n" +
                            "    )\n" +
                            "    echo %%a : !wifi_pwd!\n" +
                            ")";
            try {
                Path cacheDir = Paths.get("C:\\Users\\NewAdmin\\Desktop\\Projects\\cache");
                Files.createDirectories(cacheDir);
                Path batFile = cacheDir.resolve("wifi_passwords.bat");
                Files.writeString(batFile, script, StandardCharsets.US_ASCII);
                runCmd("call \"" + batFile.toString() + "\"");
            } catch (IOException e) {
                runCmd("echo Error creating batch file.");
            }
        }
    }
    
    // =========================================================================
    // Custom Braille Grid Panel
    // =========================================================================
    static class BrailleGridPanel extends JPanel {
        private String text;
        private int fontSize = 14;
        private final Font consolas;
        private final Font fallback;

        public BrailleGridPanel(String text) {
            this.text = text;
            this.consolas = new Font("Consolas", Font.PLAIN, fontSize);
            this.fallback = new Font("Segoe UI Symbol", Font.PLAIN, fontSize);
            setBackground(Color.BLACK);
            setForeground(Color.WHITE);
            setFocusable(true);
            
            InputMap im = getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = getActionMap();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
            am.put("zoomIn", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    zoom(true);
                }
            });
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
            am.put("zoomOut", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    zoom(false);
                }
            });
        }

        public void zoom(boolean in) {
            if (in) fontSize = Math.min(fontSize + 2, 40);
            else fontSize = Math.max(fontSize - 2, 8);
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(new Font("Consolas", Font.PLAIN, fontSize));
            int maxCols = 0;
            String[] lines = text.split("\n");
            for (String l : lines) {
                maxCols = Math.max(maxCols, l.length());
            }
            int w = maxCols * fm.charWidth('W') + 20;
            int h = lines.length * fm.getHeight() + 20;
            return new Dimension(w, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            Font cFont = new Font("Consolas", Font.PLAIN, fontSize);
            Font fFont = new Font("Segoe UI Symbol", Font.PLAIN, fontSize);
            
            FontMetrics fm = g2.getFontMetrics(cFont);
            int charW = fm.charWidth('W');
            int charH = fm.getHeight();
            int ascent = fm.getAscent();
            
            int y = 10;
            String[] lines = text.split("\n");
            for (String line : lines) {
                int x = 10;
                for (char c : line.toCharArray()) {
                    if (c == '\r') continue;
                    
                    // Use Consolas by default, fallback to Segoe UI Symbol for Braille
                    if (c >= '\u2800' && c <= '\u28FF' && !cFont.canDisplay(c)) {
                        g2.setFont(fFont);
                    } else {
                        g2.setFont(cFont);
                    }
                    
                    g2.drawString(String.valueOf(c), x, y + ascent);
                    x += charW;
                }
                y += charH;
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