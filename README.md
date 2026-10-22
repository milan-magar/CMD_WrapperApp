# 🚀 CommandCenter

A feature-rich, multi-tabbed Java Swing application that acts as a powerful wrapper for the Windows Command Prompt. It combines a terminal, file explorer, networking toolkit, Java IDE, and HTML editor into a single, cohesive, and highly customizable desktop application.

---

## ✨ Features

* **Interactive Terminal**: Fully functional command prompt with real-time input, ANSI color stripping, and direct output streaming.
* **Dynamic Theming System**: Customize the UI using a simple 2-digit color code system (e.g., `02` for Black Background and Green Text). Theme choices are saved across sessions.
* **Multi-Tab Architecture**:
  * **CMD**: Standard interactive terminal.
  * **ThisPC**: Built-in file explorer. Create, navigate, edit, and delete files/folders visually.
  * **NetworkChuck**: Pre-configured shortcut buttons for networking, system info, and curl utilities.
  * **Java** *(Hidden by default)*: A mini-IDE with line numbers, auto-indent, auto-brackets, and an interactive compile/run terminal.
  * **HTML** *(Hidden by default)*: A web dev environment with automated CSS/JS generation, link injection, and browser preview.
* **Braille & ASCII Art Rendering**: A custom rendering engine for Braille art to ensure perfect grid alignment, complete with inline color tag support (`$1` through `$9`).
* **Accurate Nepali Date Converter**: Toggle the header date between English (AD) and accurate Bikram Sambat (BS) dates.
* **Administrator Toggle**: Quick checkbox to elevate the app to run with full system privileges.
* **Session Logging**: Save terminal history to rotating log files in a `cache` directory.

---

## ⚙️ Prerequisites

* **Java JDK 11 or higher** (Required for modern Swing methods like `viewToModel2D`).
* **Windows OS** (The primary shell wrapper is designed for `cmd.exe`, though the Java/HTML editors are cross-platform).

---

## 🛠️ How to Run

1. **Clone or Download**
   Clone this repository or download the `CommandCenter.java` source file.

2. **Compile the Code**
   Open your terminal or command prompt and compile the Java file:
   ```bash
   javac CommandCenter.java
   ```

3. **Launch the Application**
   ```bash
   java CommandCenter
   ```

*(Optional)* **Run with Administrator Privileges:**
To grant full system permissions, right-click your terminal application, select **Run as Administrator**, and execute the `java CommandCenter` command.

---

## ⌨️ Keyboard Shortcuts

| Shortcut | Action |
| :--- | :--- |
| `Ctrl` + `+` / `Ctrl` + `-` | Zoom in / Zoom out (Terminal, Editors, File Explorer) |
| `Ctrl` + `Alt` + `J` | Open the hidden **Java IDE** tab |
| `Ctrl` + `Alt` + `H` | Open the hidden **HTML Editor** tab |
| `Ctrl` + `Mouse Wheel` | Fast zoom in/out on text editors and terminals |

---

## 🎨 Theme Color Guide

Type these codes into the **Theme Code** box at the bottom left and click **Apply**.

*(First digit = Background Color \| Second digit = Text Color)*

* `0`: Black
* `1`: White
* `2`: Green
* `3`: Blue
* `4`: Red
* `5`: Yellow
* `6`: Cyan
* `7`: Magenta
* `8`: Dark Gray
* `9`: Light Gray

**Examples:**
* `01` — Black BG, White Text
* `02` — Black BG, Green Text *(Default)*
* `18` — White BG, Dark Gray Text

---

## 📂 Tab Breakdown

### 1. CMD
* Fully interactive. Click directly into the terminal to type commands.
* Type `cls` to clear the screen or use the floating **CLS** button on the bottom right.

### 2. ThisPC
* Visual file navigation.
* Double-click a folder to enter it; double-click a file to edit it.
* Right-click files to **Compile** (`.java`), **Open** (Normal/Braille mode), **Rename**, or **Delete**.
* Use the top action bar to create new folders and files.

### 3. NetworkChuck
* **Left-Click**: Opens a prompt for required input (URLs, IPs, or file paths) and executes the command.
* **Right-Click**: Runs the pre-configured example command immediately.
* Includes a dynamic Wi-Fi password extractor and various `curl` utilities.

### 4. Java *(Press `Ctrl` + `Alt` + `J`)*
* Automatically manages `src` and `bin` folders based on your current project directory.
* Collapsible project file tree (toggle via `☰`).
* **Save & Run** compiles your code and launches it in the built-in terminal.
* The terminal unlocks for input when your Java program requests user input (e.g., `Scanner`).

### 5. HTML *(Press `Ctrl` + `Alt` + `H`)*
* Quick generation of `index.html`, `style.css`, and `script.js`.
* Automatically injects `<link>` and `<script>` tags into your HTML file.
* Click **Preview** to open the current HTML file in your default web browser.

---

## 🧠 Technical Highlights

* **ANSI Stripping**: The command prompt wrapper intercepts stdout/stderr streams and strips ANSI escape codes (`\u001B[...m`) to prevent terminal garbage text.
* **Custom Rendering**: Braille art is rendered using a custom `JPanel` and `Graphics2D` context rather than a `JTextArea`, ensuring precise monospaced grid alignment for complex ASCII art.
* **ANSI Inline Colors**: The Braille viewer parses `$1` to `$9` tags to dynamically change the `Graphics2D` color context without disrupting the character grid layout.
* **Thread Isolation**: File compilation, process execution, and terminal input streams run on isolated background threads to keep the Swing UI smooth and responsive.
