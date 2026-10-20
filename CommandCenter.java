import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class CommandCenter {

    public static void main(String[] args) {
        ActionStore.load();
        ThemeManager.init(ActionStore.isDark());

        if (Arrays.asList(args).contains("--elevate") && !Elevation.isElevated()) {
            try {
                if (Elevation.relaunchElevated()) System.exit(0);
            } catch (Exception ignored) {
                // fall through and run unprivileged
            }
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
            if (!Elevation.isElevated()) {
                frame.console().system(
                        "Running as a standard user. System-level commands (taskkill on protected "
                                + "processes, netsh, service control) will fail.\n"
                                + "Click the security badge in the bottom-right corner to relaunch elevated.");
            } else {
                frame.console().system("Running with administrator privileges.");
            }
        });
    }
}

/* ------------------------------------------------------------------ theme */

final class Theme {
    boolean dark;
    Color bg, panel, panelAlt, text, subtext, accent, border, fieldBg, consoleBg,
            cmdFg, errFg, sysFg, selBg, buttonBg, searchHi, ok, warn, unknown;

    static Theme dark() {
        Theme t = new Theme();
        t.dark = true;
        t.bg = new Color(0x1E1F22);
        t.panel = new Color(0x2B2D30);
        t.panelAlt = new Color(0x25272A);
        t.text = new Color(0xDFE1E5);
        t.subtext = new Color(0x9DA0A8);
        t.accent = new Color(0x4A9EFF);
        t.border = new Color(0x3C3F41);
        t.fieldBg = new Color(0x1E1F22);
        t.consoleBg = new Color(0x141518);
        t.cmdFg = new Color(0x6ACB7C);
        t.errFg = new Color(0xFF6B68);
        t.sysFg = new Color(0xD7A65A);
        t.selBg = new Color(0x2F4F78);
        t.buttonBg = new Color(0x33363A);
        t.searchHi = new Color(0x8A7B00);
        t.ok = new Color(0x2EA043);
        t.warn = new Color(0xE09B1B);
        t.unknown = new Color(0x8A8A8A);
        return t;
    }

    static Theme light() {
        Theme t = new Theme();
        t.dark = false;
        t.bg = new Color(0xF7F8FA);
        t.panel = new Color(0xFFFFFF);
        t.panelAlt = new Color(0xF0F2F5);
        t.text = new Color(0x1E1F22);
        t.subtext = new Color(0x6B6F76);
        t.accent = new Color(0x1A73E8);
        t.border = new Color(0xD7DBE0);
        t.fieldBg = new Color(0xFFFFFF);
        t.consoleBg = new Color(0xFFFFFF);
        t.cmdFg = new Color(0x0B7A33);
        t.errFg = new Color(0xC0392B);
        t.sysFg = new Color(0xB06A00);
        t.selBg = new Color(0xCFE3FF);
        t.buttonBg = new Color(0xEEF1F5);
        t.searchHi = new Color(0xFFE58A);
        t.ok = new Color(0x2EA043);
        t.warn = new Color(0xE09B1B);
        t.unknown = new Color(0x8A8A8A);
        return t;
    }
}

final class ThemeManager {

    private static Theme current = Theme.dark();
    private static boolean flatLaf = false;
    private static final List<Runnable> listeners = new ArrayList<>();

    private ThemeManager() {}

    static Theme theme() { return current; }
    static boolean flat() { return flatLaf; }

    static void addListener(Runnable r) { listeners.add(r); }

    static void init(boolean dark) {
        current = dark ? Theme.dark() : Theme.light();
        flatLaf = installFlat(dark);
        if (!flatLaf) {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (Exception ignored) {}
        }
        applyDefaults(current);
    }

    private static boolean installFlat(boolean dark) {
        try {
            UIManager.setLookAndFeel(dark
                    ? "com.formdev.flatlaf.FlatDarkLaf"
                    : "com.formdev.flatlaf.FlatLightLaf");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    static void toggle() { set(!current.dark); }

    static void set(boolean dark) {
        current = dark ? Theme.dark() : Theme.light();
        if (flatLaf) installFlat(dark);
        applyDefaults(current);

        for (Window w : Window.getWindows()) {
            if (!w.isDisplayable()) continue;
            SwingUtilities.updateComponentTreeUI(w);
            styleTree(w, current, !flatLaf);
            w.repaint();
        }
        for (Runnable r : listeners) r.run();
    }

    private static void applyDefaults(Theme t) {
        UIManager.put("Panel.background", t.panel);
        UIManager.put("Panel.foreground", t.text);
        UIManager.put("OptionPane.background", t.panel);
        UIManager.put("OptionPane.messageForeground", t.text);
        
        UIManager.put("Button.background", t.buttonBg);
        UIManager.put("Button.foreground", t.text);
        UIManager.put("Button.select", t.border);
        
        UIManager.put("Label.foreground", t.text);
        
        UIManager.put("TextField.background", t.fieldBg);
        UIManager.put("TextField.foreground", t.text);
        UIManager.put("TextField.caretForeground", t.text);
        UIManager.put("TextField.border", BorderFactory.createLineBorder(t.border));
        
        UIManager.put("ScrollPane.background", t.consoleBg);
        UIManager.put("Viewport.background", t.consoleBg);
        
        UIManager.put("Table.background", t.consoleBg);
        UIManager.put("Table.foreground", t.text);
        UIManager.put("Table.gridColor", t.border);
        UIManager.put("Table.selectionBackground", t.selBg);
        UIManager.put("Table.selectionForeground", t.text);
        UIManager.put("TableHeader.background", t.panelAlt);
        UIManager.put("TableHeader.foreground", t.text);
        
        UIManager.put("TabbedPane.background", t.panel);
        UIManager.put("TabbedPane.foreground", t.text);
        UIManager.put("TabbedPane.selected", t.panelAlt);
        UIManager.put("TabbedPane.selectedForeground", t.text);
        UIManager.put("TabbedPane.contentAreaColor", t.panel);
        UIManager.put("TabbedPane.focus", t.panel);
        
        UIManager.put("Menu.background", t.panelAlt);
        UIManager.put("Menu.foreground", t.text);
        UIManager.put("MenuItem.background", t.panelAlt);
        UIManager.put("MenuItem.foreground", t.text);
        UIManager.put("PopupMenu.background", t.panelAlt);
        UIManager.put("PopupMenu.foreground", t.text);
    }

    static void styleTree(Component c, Theme t, boolean full) {
        if (c == null) return;

        if (c instanceof JLabel l) {
            l.setForeground(t.text);
        } else if (c instanceof JButton b) {
            b.setBackground(t.buttonBg);
            b.setForeground(t.text);
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setBorderPainted(true);
        } else if (c instanceof JTextField f) {
            f.setBackground(t.fieldBg);
            f.setForeground(t.text);
            f.setCaretColor(t.text);
        } else if (c instanceof JTextPane tp) {
            tp.setBackground(t.consoleBg);
            tp.setForeground(t.text);
            tp.setCaretColor(t.text);
            tp.setSelectionColor(t.selBg);
            tp.setSelectedTextColor(t.text);
        } else if (c instanceof JTable tb) {
            tb.setBackground(t.consoleBg);
            tb.setForeground(t.text);
            tb.setGridColor(t.border);
            tb.setSelectionBackground(t.selBg);
            tb.setSelectionForeground(t.text);
            tb.getTableHeader().setBackground(t.panelAlt);
            tb.getTableHeader().setForeground(t.text);
        } else if (c instanceof JScrollPane sp) {
            sp.setBackground(t.consoleBg);
            sp.getViewport().setBackground(t.consoleBg);
            sp.setBorder(new LineBorder(t.border));
        } else if (c instanceof JPanel || c instanceof JSplitPane) {
            c.setBackground(t.panel);
        }

        if (c instanceof Container ct) {
            for (Component k : ct.getComponents()) styleTree(k, t, true);
        }
    }
}

/* --------------------------------------------------------------------- ui */

final class Ui {
    static final String MONO = pickMono();

    private Ui() {}

    private static String pickMono() {
        String[] candidates = {"JetBrains Mono", "Cascadia Mono", "Consolas", "Menlo",
                "DejaVu Sans Mono", "Monospaced"};
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String c : candidates) if (available.contains(c)) return c;
        return Font.MONOSPACED;
    }

    static Font mono(float size) {
        return new Font(MONO, Font.PLAIN, Math.round(size));
    }
}

/* ------------------------------------------------------------------ shell */

final class Shell {

    static final boolean WINDOWS =
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    static final boolean MAC =
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");
    static final Charset CS = WINDOWS ? StandardCharsets.UTF_8 : Charset.defaultCharset();

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "shell-worker");
        t.setDaemon(true);
        return t;
    });

    interface Listener {
        void onStart(String command);
        void onLine(String line, boolean err);
        void onEnd(int exit, long millis);
    }

    record Res(int exit, List<String> out) {}

    private Shell() {}

    private static ProcessBuilder builder(String command) {
        if (WINDOWS) {
            return new ProcessBuilder("cmd.exe", "/d", "/c",
                    "chcp 65001>nul 2>&1 & " + command);
        }
        String sh = Files.isExecutable(Paths.get("/bin/bash")) ? "/bin/bash" : "/bin/sh";
        return new ProcessBuilder(sh, "-c", command);
    }

    static void run(String command, Listener listener) {
        POOL.submit(() -> {
            long start = System.nanoTime();
            SwingUtilities.invokeLater(() -> listener.onStart(command));
            int exit = -1;
            Process p = null;
            try {
                p = builder(command).start();
                p.getOutputStream().close();
                Thread out = pump(p.getInputStream(), false, listener);
                Thread err = pump(p.getErrorStream(), true, listener);
                exit = p.waitFor();
                out.join(2000);
                err.join(2000);
            } catch (Exception e) {
                String m = String.valueOf(e.getMessage());
                SwingUtilities.invokeLater(() -> listener.onLine(m, true));
            } finally {
                if (p != null) p.destroy();
            }
            int code = exit;
            long ms = (System.nanoTime() - start) / 1_000_000L;
            SwingUtilities.invokeLater(() -> listener.onEnd(code, ms));
        });
    }

    private static Thread pump(InputStream in, boolean err, Listener listener) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, CS))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String s = line;
                    SwingUtilities.invokeLater(() -> listener.onLine(s, err));
                }
            } catch (IOException ignored) {}
        }, "shell-pump");
        t.setDaemon(true);
        t.start();
        return t;
    }

    static void runAsync(String command, Consumer<List<String>> done) {
        POOL.submit(() -> {
            List<String> lines = new ArrayList<>();
            try {
                Process p = builder(command).start();
                p.getOutputStream().close();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), CS))) {
                    String l;
                    while ((l = r.readLine()) != null) lines.add(l);
                }
                p.waitFor();
            } catch (Exception e) {
                lines.add("! " + e.getMessage());
            }
            SwingUtilities.invokeLater(() -> done.accept(lines));
        });
    }

    static Res exec(List<String> cmd, int timeoutSec) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            p.getOutputStream().close();
            List<String> lines = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), CS))) {
                String l;
                while ((l = r.readLine()) != null) lines.add(l);
            }
            if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return new Res(-1, lines);
            }
            return new Res(p.exitValue(), lines);
        } catch (Exception e) {
            return new Res(-1, List.of(String.valueOf(e.getMessage())));
        }
    }
}

/* -------------------------------------------------------------- elevation */

final class Elevation {

    enum Level { ELEVATED, STANDARD, UNKNOWN }

    private static Level cached;

    private Elevation() {}

    static Level level() {
        if (cached == null) cached = detect();
        return cached;
    }

    static boolean isElevated() { return level() == Level.ELEVATED; }

    static void reset() { cached = null; }

    private static Level detect() {
        try {
            if (Shell.WINDOWS) {
                Shell.Res r = Shell.exec(List.of("whoami", "/groups"), 6);
                String all = String.join("\n", r.out());
                if (all.contains("S-1-16-12288")) return Level.ELEVATED;
                if (all.contains("S-1-16-8192")) return Level.STANDARD;
                return Shell.exec(List.of("net", "session"), 6).exit() == 0
                        ? Level.ELEVATED : Level.STANDARD;
            }
            Shell.Res r = Shell.exec(List.of("id", "-u"), 6);
            return r.exit() == 0 && !r.out().isEmpty() && r.out().get(0).trim().equals("0")
                    ? Level.ELEVATED : Level.STANDARD;
        } catch (Exception e) {
            return Level.UNKNOWN;
        }
    }

    static boolean relaunchElevated() throws Exception {
        List<String> args = buildJavaCommand();

        if (Shell.WINDOWS) {
            StringBuilder ps = new StringBuilder("Start-Process -Verb RunAs -FilePath ")
                    .append(psq(args.get(0)))
                    .append(" -WorkingDirectory ")
                    .append(psq(System.getProperty("user.dir")))
                    .append(" -ArgumentList @(");
            for (int i = 1; i < args.size(); i++) {
                if (i > 1) ps.append(", ");
                ps.append(psq(args.get(i)));
            }
            ps.append(")");
            Process p = new ProcessBuilder("powershell", "-NoProfile",
                    "-WindowStyle", "Hidden", "-Command", ps.toString()).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        }

        if (Shell.MAC) {
            StringBuilder sh = new StringBuilder("cd ")
                    .append(shq(System.getProperty("user.dir"))).append(" && ");
            for (String a : args) sh.append(shq(a)).append(' ');
            String script = "do shell script \"" + esc(sh.toString())
                    + "\" with administrator privileges";
            Process p = new ProcessBuilder("osascript", "-e", script).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        }

        List<String> pk = new ArrayList<>();
        pk.add("pkexec");
        pk.addAll(args);
        Process p = new ProcessBuilder(pk).start();
        p.getInputStream().readAllBytes();
        int rc = p.waitFor();
        return rc == 0;
    }

    private static List<String> buildJavaCommand() {
        String javaExe = ProcessHandle.current().info().command().orElseGet(() ->
                Paths.get(System.getProperty("java.home"), "bin",
                        Shell.WINDOWS ? "java.exe" : "java").toString());

        String prop = System.getProperty("sun.java.command", "").trim();
        String target = prop.isEmpty() ? "" : prop.split("\\s+")[0];

        List<String> args = new ArrayList<>();
        args.add(javaExe);
        if (target.endsWith(".jar")) {
            args.add("-jar");
            args.add(target);
        } else {
            args.add("-cp");
            args.add(System.getProperty("java.class.path", "."));
            if (!target.isEmpty()) args.add(target);
        }
        args.add("--elevate");
        return args;
    }

    private static String psq(String s) { return "'" + s.replace("'", "''") + "'"; }
    private static String shq(String s) { return "'" + s.replace("'", "'\\''") + "'"; }
    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

/* ---------------------------------------------------------------- actions */

record ActionItem(String label, String command, String category) {}

final class ActionStore {

    private static final Path DIR = Paths.get(System.getProperty("user.home"), ".commandcenter");
    private static final Path FILE = DIR.resolve("settings.properties");

    private static final List<ActionItem> CUSTOM = new ArrayList<>();
    private static boolean darkTheme = true;

    private ActionStore() {}

    static List<ActionItem> custom() { return CUSTOM; }
    static boolean isDark() { return darkTheme; }
    static void setDark(boolean dark) { darkTheme = dark; save(); }

    static List<ActionItem> defaults() {
        boolean w = Shell.WINDOWS;
        List<ActionItem> a = new ArrayList<>();

        // Original Categories
        a.add(new ActionItem("Ping Google DNS",
                w ? "ping -n 4 8.8.8.8" : "ping -c 4 8.8.8.8", "Network"));
        a.add(new ActionItem("Ping Cloudflare DNS",
                w ? "ping -n 4 1.1.1.1" : "ping -c 4 1.1.1.1", "Network"));
        a.add(new ActionItem("IP configuration",
                w ? "ipconfig /all" : "ifconfig -a", "Network"));
        a.add(new ActionItem("Active connections",
                w ? "netstat -ano" : "netstat -an", "Network"));
        a.add(new ActionItem("Traceroute to 8.8.8.8",
                w ? "tracert 8.8.8.8" : "traceroute 8.8.8.8", "Network"));
        a.add(new ActionItem("DNS lookup",
                w ? "nslookup google.com" : "dig google.com", "Network"));

        a.add(new ActionItem("System information",
                w ? "systeminfo" : "uname -a", "System"));
        a.add(new ActionItem("Disk space",
                w ? "wmic logicaldisk get caption,size,freespace" : "df -h", "System"));
        a.add(new ActionItem("Environment variables",
                w ? "set" : "env", "System"));
        a.add(new ActionItem("Who am I",
                w ? "whoami /all" : "id", "System"));
        a.add(new ActionItem("Uptime",
                w ? "net statistics workstation" : "uptime", "System"));
        a.add(new ActionItem("Services",
                w ? "sc query state= all" : "systemctl list-units --type=service --no-pager",
                "System"));

        a.add(new ActionItem("Java version", "java -version", "Dev"));
        a.add(new ActionItem("Git status", "git status", "Dev"));
        a.add(new ActionItem("Current directory", w ? "cd" : "pwd", "Dev"));
        a.add(new ActionItem("List files", w ? "dir" : "ls -la", "Dev"));
        a.add(new ActionItem("Environment PATH",
                w ? "echo %PATH%" : "echo $PATH", "Dev"));

        // NetworkChuck Basic (Video 1: 40 Windows Commands you NEED to know)
        a.add(new ActionItem("NC: IP Config All", "ipconfig /all", "NC-Basic"));
        a.add(new ActionItem("NC: Flush DNS Cache", "ipconfig /flushdns", "NC-Basic"));
        a.add(new ActionItem("NC: Display DNS Cache", "ipconfig /displaydns", "NC-Basic"));
        a.add(new ActionItem("NC: Release & Renew IP", "ipconfig /release && ipconfig /renew", "NC-Basic"));
        a.add(new ActionItem("NC: NSLookup Google", "nslookup google.com", "NC-Basic"));
        a.add(new ActionItem("NC: Get MAC Addresses", "getmac /v", "NC-Basic"));
        a.add(new ActionItem("NC: Power Energy Report", "powercfg /energy", "NC-Basic"));
        a.add(new ActionItem("NC: Battery Report", "powercfg /batteryreport", "NC-Basic"));
        a.add(new ActionItem("NC: System File Checker (SFC)", "sfc /scannow", "NC-Basic"));
        a.add(new ActionItem("NC: DISM Restore Health", "dism /online /cleanup-image /restorehealth", "NC-Basic"));
        a.add(new ActionItem("NC: Check Disk (/f)", "chkdsk /f", "NC-Basic"));
        a.add(new ActionItem("NC: Wireless WLAN Report", "netsh wlan show wlan report", "NC-Basic"));
        a.add(new ActionItem("NC: Continuous Ping (8.8.8.8)", "ping 8.8.8.8 -t", "NC-Basic"));
        a.add(new ActionItem("NC: Traceroute (-d)", "tracert -d 8.8.8.8", "NC-Basic"));
        a.add(new ActionItem("NC: Netstat Active Ports", "netstat -ano", "NC-Basic"));
        a.add(new ActionItem("NC: Routing Table (Route Print)", "route print", "NC-Basic"));
        a.add(new ActionItem("NC: Reboot to BIOS", "shutdown /r /fw", "NC-Basic"));

        // NetworkChuck Advanced (Video 2: 30 Windows Commands you CAN'T live without)
        a.add(new ActionItem("NC: Show Wi-Fi Profiles", "netsh wlan show profiles", "NC-Advanced"));
        a.add(new ActionItem("NC: Check Weather (wttr.in)", "curl wttr.in", "NC-Advanced"));
        a.add(new ActionItem("NC: Public IP (AWS)", "curl checkip.amazonaws.com", "NC-Advanced"));
        a.add(new ActionItem("NC: Check Website Status", "curl -I -s https://networkchuck.com", "NC-Advanced"));
        a.add(new ActionItem("NC: Generate QR Code", "curl qrcode.show", "NC-Advanced"));
        a.add(new ActionItem("NC: Command History (Doskey)", "doskey /history", "NC-Advanced"));
        a.add(new ActionItem("NC: System Information", "systeminfo", "NC-Advanced"));
        a.add(new ActionItem("NC: Open Explorer Here", "explorer .", "NC-Advanced"));
        a.add(new ActionItem("NC: Set Terminal Color", "color 02", "NC-Advanced"));
        a.add(new ActionItem("NC: Set Terminal Title", "title Home Sweet Home", "NC-Advanced"));
        a.add(new ActionItem("NC: Star Wars via Telnet", "telnet towel.blinkenlights.nl", "NC-Advanced"));

        return a;
    }

    static void addCustom(ActionItem item) { CUSTOM.add(item); save(); }
    static void removeCustom(ActionItem item) { CUSTOM.remove(item); save(); }

    static void load() {
        if (!Files.isRegularFile(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        darkTheme = !"light".equalsIgnoreCase(p.getProperty("theme", "dark"));
        int n = parseInt(p.getProperty("custom.count", "0"));
        for (int i = 0; i < n; i++) {
            String label = p.getProperty("custom." + i + ".label");
            String command = p.getProperty("custom." + i + ".command");
            if (label != null && command != null && !label.isBlank() && !command.isBlank()) {
                CUSTOM.add(new ActionItem(label, command, "Custom"));
            }
        }
    }

    static void save() {
        Properties p = new Properties();
        p.setProperty("theme", darkTheme ? "dark" : "light");
        p.setProperty("custom.count", String.valueOf(CUSTOM.size()));
        for (int i = 0; i < CUSTOM.size(); i++) {
            p.setProperty("custom." + i + ".label", CUSTOM.get(i).label());
            p.setProperty("custom." + i + ".command", CUSTOM.get(i).command());
        }
        try {
            Files.createDirectories(DIR);
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Command Center settings");
            }
        } catch (IOException e) {
            System.err.println("Could not save settings: " + e.getMessage());
        }
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}

/* ------------------------------------------------------------------ badge */

final class SecurityBadge extends JComponent {

    Runnable onClick;
    private Elevation.Level level = Elevation.level();

    SecurityBadge() {
        setOpaque(false);
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        setFont(base.deriveFont(Font.BOLD, 11f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });
        refresh();
    }

    void refresh() {
        level = Elevation.level();
        setToolTipText(switch (level) {
            case ELEVATED -> "Running as Administrator — full access to protected commands.";
            case STANDARD -> "Standard user — protected commands will fail. Click to relaunch elevated.";
            case UNKNOWN -> "Privilege level could not be determined.";
        });
        revalidate();
        repaint();
    }

    private String text() {
        return switch (level) {
            case ELEVATED -> "Administrator";
            case STANDARD -> "Standard user";
            case UNKNOWN -> "Unknown";
        };
    }

    private Color base() {
        Theme t = ThemeManager.theme();
        return switch (level) {
            case ELEVATED -> t.ok;
            case STANDARD -> t.warn;
            case UNKNOWN -> t.unknown;
        };
    }

    @Override public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(text()) + 36, fm.getHeight() + 8);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color c = base();
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
        g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

        g2.fillOval(10, h / 2 - 4, 8, 8);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text(), 24, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}

/* ---------------------------------------------------------------- console */

final class ConsolePanel extends JPanel {

    private record Entry(String role, String text) {}

    private static final int MAX_ENTRIES = 20000;

    private final JTextPane out = new JTextPane();
    private final JTextField input = new JTextField();
    private final JScrollPane scroll;
    private final List<Entry> log = new ArrayList<>();
    private final List<String> history = new ArrayList<>();

    private int histPos;
    private boolean rebuilding;
    private String searchTerm = "";
    private Consumer<String> onSubmit = s -> { };

    private SimpleAttributeSet stOut, stErr, stCmd, stSys;

    ConsolePanel() {
        super(new BorderLayout());

        out.setEditable(false);
        out.setFont(Ui.mono(13));
        out.setMargin(new Insets(10, 12, 10, 12));
        out.setBorder(null);

        scroll = new JScrollPane(out);
        scroll.setBorder(new LineBorder(ThemeManager.theme().border));
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        JLabel prompt = new JLabel(">");
        prompt.setFont(Ui.mono(14));
        prompt.setBorder(new EmptyBorder(0, 4, 0, 6));

        input.setFont(Ui.mono(13));
        input.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.theme().border),
                new EmptyBorder(6, 8, 6, 8)));

        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.setBorder(new EmptyBorder(8, 10, 8, 10));
        inputRow.add(prompt, BorderLayout.WEST);
        inputRow.add(input, BorderLayout.CENTER);

        add(scroll, BorderLayout.CENTER);
        add(inputRow, BorderLayout.SOUTH);

        input.addActionListener(e -> fire());

        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "histUp");
        input.getActionMap().put("histUp", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { historyPrev(); }
        });
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "histDown");
        input.getActionMap().put("histDown", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { historyNext(); }
        });
        input.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "clearLog");
        input.getActionMap().put("clearLog", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { clear(); }
        });

        refreshStyles();
    }

    void setOnSubmit(Consumer<String> c) { onSubmit = c; }

    void command(String cmd) { append("cmd", "> " + cmd); }
    void line(String text, boolean err) { append(err ? "err" : "out", text); }
    void system(String text) { append("sys", text); }

    String text() { return out.getText(); }

    void clear() {
        log.clear();
        out.setText("");
        searchHighlight(searchTerm);
    }

    void highlight(String term) {
        searchTerm = term == null ? "" : term;
        searchHighlight(searchTerm);
    }

    void refreshStyles() {
        Theme t = ThemeManager.theme();
        stOut = style(t.text);
        stErr = style(t.errFg);
        stCmd = style(t.cmdFg);
        stSys = style(t.sysFg);
        stCmd.addAttribute(StyleConstants.Bold, Boolean.TRUE);
        stSys.addAttribute(StyleConstants.Italic, Boolean.TRUE);
        rebuild();
    }

    private static SimpleAttributeSet style(Color fg) {
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setForeground(s, fg);
        StyleConstants.setFontFamily(s, Ui.MONO);
        StyleConstants.setFontSize(s, 13);
        return s;
    }

    private AttributeSet styleFor(String role) {
        return switch (role) {
            case "cmd" -> stCmd;
            case "err" -> stErr;
            case "sys" -> stSys;
            default -> stOut;
        };
    }

    private void append(String role, String text) {
        log.add(new Entry(role, text));
        if (log.size() > MAX_ENTRIES) {
            log.subList(0, log.size() - MAX_ENTRIES).clear();
            rebuild();
            return;
        }
        insert(role, text);
    }

    private void insert(String role, String text) {
        boolean stick = !rebuilding && atBottom();
        StyledDocument d = out.getStyledDocument();
        try {
            if (d.getLength() > 0) d.insertString(d.getLength(), "\n", null);
            d.insertString(d.getLength(), text, styleFor(role));
        } catch (BadLocationException ignored) {}
        if (stick) out.setCaretPosition(d.getLength());
    }

    private void rebuild() {
        rebuilding = true;
        try {
            out.setText("");
            for (Entry e : log) insert(e.role(), e.text());
        } finally {
            rebuilding = false;
        }
        out.setCaretPosition(out.getDocument().getLength());
        searchHighlight(searchTerm);
    }

    private void searchHighlight(String term) {
        Highlighter h = out.getHighlighter();
        h.removeAllHighlights();
        if (term == null || term.isBlank()) return;

        Document d = out.getDocument();
        String content;
        try {
            content = d.getText(0, d.getLength());
        } catch (BadLocationException e) {
            return;
        }
        Highlighter.HighlightPainter painter =
                new DefaultHighlighter.DefaultHighlightPainter(ThemeManager.theme().searchHi);
        int i = 0;
        while ((i = content.indexOf(term, i)) >= 0) {
            try {
                h.addHighlight(i, i + term.length(), painter);
            } catch (BadLocationException ignored) {}
            i += Math.max(1, term.length());
        }
    }

    private boolean atBottom() {
        JScrollBar sb = scroll.getVerticalScrollBar();
        return sb.getValue() + sb.getVisibleAmount() >= sb.getMaximum() - 16;
    }

    private void fire() {
        String cmd = input.getText().trim();
        if (cmd.isEmpty()) return;
        input.setText("");
        if (history.isEmpty() || !history.get(history.size() - 1).equals(cmd)) {
            history.add(cmd);
        }
        if (history.size() > 500) history.remove(0);
        histPos = history.size();
        onSubmit.accept(cmd);
    }

    private void historyPrev() {
        if (history.isEmpty()) return;
        histPos = Math.max(0, histPos - 1);
        input.setText(history.get(histPos));
        input.setCaretPosition(input.getText().length());
    }

    private void historyNext() {
        if (history.isEmpty()) return;
        histPos = Math.min(history.size(), histPos + 1);
        input.setText(histPos >= history.size() ? "" : history.get(histPos));
        input.setCaretPosition(input.getText().length());
    }
}

/* ---------------------------------------------------------------- process */

final class ProcessPanel extends JPanel {

    private final MainFrame owner;
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField filterField = new JTextField(16);
    private final JLabel info = new JLabel(" ");
    private final JButton refreshButton = new JButton("Refresh");
    private final TableRowSorter<DefaultTableModel> sorter;

    ProcessPanel(MainFrame owner) {
        super(new BorderLayout(0, 8));
        this.owner = owner;
        setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] cols = Shell.WINDOWS
                ? new String[]{"PID", "Name", "Session", "Memory"}
                : new String[]{"PID", "User", "CPU%", "MEM%", "Command"};

        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setMaxWidth(90);

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        topLeft.add(refreshButton);
        topLeft.add(new JLabel("Filter:"));
        topLeft.add(filterField);

        JPanel top = new JPanel(new BorderLayout());
        top.add(topLeft, BorderLayout.WEST);
        top.add(info, BorderLayout.EAST);

        refreshButton.addActionListener(e -> refreshList());

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        JPopupMenu popup = new JPopupMenu();
        JMenuItem kill = new JMenuItem("End process…");
        kill.addActionListener(e -> killSelected());
        popup.add(kill);
        table.setComponentPopupMenu(popup);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { selectRowAt(e); }
            @Override public void mouseReleased(MouseEvent e) { selectRowAt(e); }
        });

        JButton killButton = new JButton("End selected process");
        killButton.addActionListener(e -> killSelected());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bottom.add(killButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    void maybeRefresh() {
        if (model.getRowCount() == 0) refreshList();
    }

    void refreshList() {
        refreshButton.setEnabled(false);
        info.setText("Loading…");
        String cmd = Shell.WINDOWS
                ? "tasklist /fo csv /nh"
                : "ps -eo pid,user,pcpu,pmem,comm";
        Shell.runAsync(cmd, this::populate);
    }

    private void populate(List<String> lines) {
        model.setRowCount(0);
        int count = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            if (Shell.WINDOWS) {
                List<String> f = parseCsv(line);
                if (f.size() < 5) continue;
                if (f.get(1).equalsIgnoreCase("PID")) continue;
                model.addRow(new Object[]{f.get(1).trim(), f.get(0).trim(),
                        f.get(2).trim(), f.get(4).trim()});
                count++;
            } else {
                String[] p = line.trim().split("\\s+", 5);
                if (p.length < 5 || !p[0].matches("\\d+")) continue;
                model.addRow(new Object[]{p[0], p[1], p[2], p[3], p[4]});
                count++;
            }
        }
        info.setText(count + " processes");
        refreshButton.setEnabled(true);
    }

    private static List<String> parseCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    sb.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        out.add(sb.toString());
        return out;
    }

    private void applyFilter() {
        String text = filterField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
    }

    private void selectRowAt(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        if (row >= 0 && !table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
    }

    private void killSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a process first.",
                    "No selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        String pid = String.valueOf(model.getValueAt(row, 0));
        String name = String.valueOf(model.getValueAt(row, 1));

        int r = JOptionPane.showConfirmDialog(this,
                "End process " + name + " (PID " + pid + ")?",
                "Confirm", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String cmd = Shell.WINDOWS
                ? "taskkill /PID " + pid + " /F"
                : "kill -9 " + pid;
        owner.runCommand(cmd, false);

        Timer t = new Timer(1200, e -> {
            ((Timer) e.getSource()).stop();
            refreshList();
        });
        t.setRepeats(false);
        t.start();
    }
}

/* ------------------------------------------------------------------- main */

final class MainFrame extends JFrame {

    private static final int S_IDLE = 0;
    private static final int S_RUN = 1;
    private static final int S_ERR = 2;

    private final ConsolePanel console = new ConsolePanel();
    private final ProcessPanel processes = new ProcessPanel(this);
    private final JTabbedPane left = new JTabbedPane();
    private final JTabbedPane right = new JTabbedPane();
    private final SecurityBadge badge = new SecurityBadge();
    private final JLabel statusLabel = new JLabel("Ready");
    private final JTextField search = new JTextField(14);
    private final JPanel customList = new JPanel();
    private final Timer searchTimer;

    private int statusKind = S_IDLE;

    MainFrame() {
        super("Command Center — Swing command-prompt wrapper");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 620));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        console.setOnSubmit(cmd -> runCommand(cmd, true));
        badge.onClick = this::offerElevation;
        statusLabel.putClientProperty("exempt", Boolean.TRUE);

        searchTimer = new Timer(250, e -> console.highlight(search.getText()));
        searchTimer.setRepeats(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { searchTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { searchTimer.restart(); }
        });

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        ThemeManager.addListener(this::applyTheme);
        applyTheme();

        right.addChangeListener(e -> {
            if (right.getSelectedComponent() == processes) processes.maybeRefresh();
        });
    }

    ConsolePanel console() { return console; }

    /* ------------------------------------------------------------- layout */

    private JComponent buildHeader() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel title = new JLabel("Command Center");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        JLabel subtitle = new JLabel("  Swing wrapper for the system shell");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));

        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftBox.setOpaque(false);
        leftBox.add(title);
        leftBox.add(subtitle);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBox.setOpaque(false);
        rightBox.add(new JLabel("Find:"));
        rightBox.add(search);
        rightBox.add(headerButton("Copy", "Copy the console to the clipboard", () -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(console.text()), null);
            setStatus("Console copied to clipboard", S_IDLE);
        }));
        rightBox.add(headerButton("Save log…", "Save the console to a text file", this::saveLog));
        rightBox.add(headerButton("Clear", "Clear the console (Ctrl+L in the input bar)",
                console::clear));
        rightBox.add(headerButton("Theme", "Toggle dark / light theme", () -> {
            ThemeManager.toggle();
            ActionStore.setDark(ThemeManager.theme().dark);
        }));

        bar.add(leftBox, BorderLayout.WEST);
        bar.add(rightBox, BorderLayout.EAST);
        return bar;
    }

    private JButton headerButton(String text, String tip, Runnable action) {
        JButton b = new JButton(text);
        b.setToolTipText(tip);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.theme().border),
                new EmptyBorder(5, 10, 5, 10)));
        b.addActionListener(e -> action.run());
        return b;
    }

    private JComponent buildCenter() {
        left.addTab("Network", scrollPane(categoryPanel("Network")));
        left.addTab("System", scrollPane(categoryPanel("System")));
        left.addTab("Dev & Files", scrollPane(devPanel()));

        // NetworkChuck Nested Tab Group (Basic = Video 1, Advanced = Video 2)
        JTabbedPane ncTabs = new JTabbedPane();
        ncTabs.addTab("Basic", scrollPane(networkChuckPanel("NC-Basic")));
        ncTabs.addTab("Advanced", scrollPane(networkChuckPanel("NC-Advanced")));
        left.addTab("NetworkChuck", ncTabs);

        left.addTab("Custom", customPanel());

        right.addTab("Console", console);
        right.addTab("Processes", processes);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(340);
        split.setResizeWeight(0.0);
        split.setDividerSize(6);
        split.setBorder(null);
        return split;
    }

    private JScrollPane scrollPane(JPanel content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JPanel categoryPanel(String category) {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(10, 10, 10, 10));
        for (ActionItem a : ActionStore.defaults()) {
            if (!a.category().equals(category)) continue;
            JButton b = toolButton(a.label(), a.command());
            b.addActionListener(e -> runCommand(a.command(), true));
            list.add(b);
            list.add(Box.createVerticalStrut(6));
        }
        list.add(Box.createVerticalGlue());
        return list;
    }

    private JPanel networkChuckPanel(String category) {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(10, 10, 10, 10));
        for (ActionItem a : ActionStore.defaults()) {
            if (!a.category().equals(category)) continue;
            JButton b = toolButton(a.label(), a.command());
            b.addActionListener(e -> runCommand(a.command(), true));
            list.add(b);
            list.add(Box.createVerticalStrut(6));
        }
        list.add(Box.createVerticalGlue());
        return list;
    }

    private JPanel devPanel() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(10, 10, 10, 10));

        for (ActionItem a : ActionStore.defaults()) {
            if (!a.category().equals("Dev")) continue;
            JButton b = toolButton(a.label(), a.command());
            b.addActionListener(e -> runCommand(a.command(), true));
            list.add(b);
            list.add(Box.createVerticalStrut(6));
        }

        JButton hash = toolButton("Hash a file (SHA-256)…",
                "Choose a file and compute its SHA-256 digest");
        hash.addActionListener(e -> hashFile());
        list.add(hash);
        list.add(Box.createVerticalStrut(6));

        JButton count = toolButton("Count files in a folder…",
                "Choose a folder and count the files it contains");
        count.addActionListener(e -> countFiles());
        list.add(count);

        list.add(Box.createVerticalGlue());
        return list;
    }

    private JComponent customPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton add = toolButton("+  Add action…", "Create a new custom shortcut button");
        add.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        add.addActionListener(e -> addCustomAction());

        customList.setLayout(new BoxLayout(customList, BoxLayout.Y_AXIS));
        customList.setBorder(new EmptyBorder(4, 0, 4, 0));
        refreshCustomList();

        JPanel holder = new JPanel(new BorderLayout());
        holder.add(customList, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(holder);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        JLabel hint = new JLabel("<html><small>Stored in ~/.commandcenter/settings.properties. "
                + "Right-click a button to remove it.</small></html>");

        root.add(add, BorderLayout.NORTH);
        root.add(sp, BorderLayout.CENTER);
        root.add(hint, BorderLayout.SOUTH);
        return root;
    }

    private JButton toolButton(String label, String tooltip) {
        JButton b = new JButton(label);
        b.setToolTipText(tooltip);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        b.setPreferredSize(new Dimension(280, 36));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.theme().border),
                new EmptyBorder(4, 10, 4, 10)));
        return b;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBorder(new EmptyBorder(6, 14, 6, 14));
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(badge, BorderLayout.EAST);
        return bar;
    }

    /* ------------------------------------------------------------- actions */

    void runCommand(String command, boolean showConsole) {
        if (command == null || command.isBlank()) return;
        if (showConsole) right.setSelectedComponent(console);
        console.command(command);
        setStatus("Running: " + abbreviate(command), S_RUN);

        Shell.run(command, new Shell.Listener() {
            @Override public void onStart(String c) {}

            @Override public void onLine(String line, boolean err) {
                console.line(line, err);
            }

            @Override public void onEnd(int exit, long millis) {
                console.system("[exit " + exit + "  ·  " + millis + " ms]");
                if (exit == 0) {
                    setStatus("Ready", S_IDLE);
                } else {
                    setStatus("Last command exited with code " + exit, S_ERR);
                }
            }
        });
    }

    private void saveLog() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("command-center-log.txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Files.writeString(fc.getSelectedFile().toPath(), console.text(),
                    StandardCharsets.UTF_8);
            setStatus("Log saved to " + fc.getSelectedFile().getName(), S_IDLE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void hashFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose a file to hash");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String path = fc.getSelectedFile().getAbsolutePath();
        runCommand(Shell.WINDOWS
                ? "certutil -hashfile \"" + path + "\" SHA256"
                : "shasum -a 256 \"" + path + "\"", true);
    }

    private void countFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Choose a folder");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String path = fc.getSelectedFile().getAbsolutePath();
        runCommand(Shell.WINDOWS
                ? "dir /s /a-d \"" + path + "\" | find /c \"File(s)\""
                : "find \"" + path + "\" -type f | wc -l", true);
    }

    private void addCustomAction() {
        JTextField label = new JTextField(28);
        JTextField command = new JTextField(28);

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Button label:"));
        form.add(label);
        form.add(new JLabel("Command to run (shell syntax is allowed):"));
        form.add(command);

        int r = JOptionPane.showConfirmDialog(this, form, "New custom action",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String l = label.getText().trim();
        String c = command.getText().trim();
        if (l.isEmpty() || c.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both fields are required.",
                    "Invalid input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ActionStore.addCustom(new ActionItem(l, c, "Custom"));
        refreshCustomList();
        left.setSelectedIndex(4);
    }

    private void refreshCustomList() {
        customList.removeAll();

        if (ActionStore.custom().isEmpty()) {
            JLabel empty = new JLabel("<html><i>No custom actions yet — click “Add action…” above.</i></html>");
            empty.setBorder(new EmptyBorder(8, 2, 8, 2));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            customList.add(empty);
        }

        for (ActionItem a : ActionStore.custom()) {
            JButton b = toolButton(a.label(), a.command());
            b.addActionListener(e -> runCommand(a.command(), true));

            JPopupMenu menu = new JPopupMenu();
            JMenuItem remove = new JMenuItem("Remove “" + a.label() + "”");
            remove.addActionListener(e -> {
                int ok = JOptionPane.showConfirmDialog(this,
                        "Remove the button “" + a.label() + "”?",
                        "Remove action", JOptionPane.OK_CANCEL_OPTION);
                if (ok == JOptionPane.OK_OPTION) {
                    ActionStore.removeCustom(a);
                    refreshCustomList();
                }
            });
            menu.add(remove);
            b.setComponentPopupMenu(menu);

            customList.add(b);
            customList.add(Box.createVerticalStrut(6));
        }

        customList.add(Box.createVerticalGlue());
        customList.revalidate();
        customList.repaint();
    }

    /* ----------------------------------------------------------- elevation */

    private void offerElevation() {
        if (Elevation.isElevated()) {
            JOptionPane.showMessageDialog(this,
                    "Command Center is already running with administrator rights.",
                    "Elevated", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "Relaunch Command Center with administrator rights?\n\n"
                        + "The OS will show a UAC / authentication prompt. The current\n"
                        + "window closes and a new elevated instance starts — console\n"
                        + "history is not carried over.",
                "Elevate privileges", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        try {
            if (Elevation.relaunchElevated()) {
                System.exit(0);
            } else {
                console.system("[elevation cancelled or failed]");
            }
        } catch (Exception ex) {
            console.system("[elevation failed: " + ex.getMessage() + "]");
        }
    }

    /* --------------------------------------------------------------- theme */

    private void applyTheme() {
        Theme t = ThemeManager.theme();
        getContentPane().setBackground(t.bg);
        console.refreshStyles();
        ThemeManager.styleTree(getContentPane(), t, !ThemeManager.flat());
        applyStatusColor();
        repaint();
    }

    private void setStatus(String text, int kind) {
        statusLabel.setText(text);
        statusKind = kind;
        applyStatusColor();
    }

    private void applyStatusColor() {
        Theme t = ThemeManager.theme();
        statusLabel.setForeground(
                statusKind == S_RUN ? t.sysFg :
                statusKind == S_ERR ? t.errFg : t.subtext);
    }

    private static String abbreviate(String s) {
        return s.length() > 70 ? s.substring(0, 67) + "…" : s;
    }
}