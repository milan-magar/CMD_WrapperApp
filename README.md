# CommandCenter

A feature-rich, multi-tabbed Java Swing application that acts as a powerful wrapper for the Windows Command Prompt. It combines a terminal, file explorer, networking toolkit, Java IDE, and HTML editor into a single, cohesive, and highly customizable desktop application.

## 🚀 Features

*   **Interactive Terminal**: Fully functional command prompt with real-time input, ANSI color stripping, and direct output streaming.
*   **Dynamic Theming System**: Customize the UI using a simple 2-digit color code system (e.g., `02` for Black Background and Green Text). Theme choices are saved across sessions.
*   **Multi-Tab Architecture**: 
    *   **CMD**: Standard interactive terminal.
    *   **ThisPC**: Built-in file explorer. Create, navigate, edit, and delete files/folders visually.
    *   **NetworkChuck**: Pre-configured shortcut buttons for networking, system info, and curl utilities.
    *   **Java** (Hidden by default): A mini-IDE with line numbers, auto-indent, auto-brackets, and an interactive compile/run terminal.
    *   **HTML** (Hidden by default): A web dev environment with automated CSS/JS generation, link injection, and browser preview.
*   **Braille & ASCII Art Rendering**: A custom rendering engine for Braille art to ensure perfect grid alignment, complete with inline color tag support (`$1` through `$9`).
*   **Accurate Nepali Date Converter**: Toggle the header date between English (AD) and accurate Bikram Sambat (BS) dates.
*   **Administrator Toggle**: Quick checkbox to elevate the app to run with full system privileges.
*   **Session Logging**: Save terminal history to rotating log files in a `cache` directory.

## ⚙️ Prerequisites

*   **Java JDK 11 or higher** (Required for modern Swing methods like `viewToModel2D`).
*   **Windows OS** (The primary shell wrapper is designed for `cmd.exe`, though the Java/HTML editors are cross-platform).

## 🛠️ How to Run

1. Clone this repository or download the `CommandCenter.java` file.
2. Open your terminal or command prompt.
3. Compile the Java file:
   ```bash
   javac CommandCenter.java
Run the application:

bash
java CommandCenter
(Optional) To run with Administrator privileges for full system access:
Right-click your terminal -> Select "Run as Administrator" -> Run the java command.

⌨️ Keyboard Shortcuts
Shortcut	Action
Ctrl + + / Ctrl + -	Zoom in / Zoom out (Terminal, Editors, File Explorer)
Ctrl + Alt + J	Open the hidden Java IDE tab
Ctrl + Alt + H	Open the hidden HTML Editor tab
Ctrl + Mouse Wheel	Fast zoom in/out on text editors and terminals
🎨 Theme Color Guide
Type these codes into the Theme Code box at the bottom left and click Apply.
(First digit = Background Color | Second digit = Text Color)

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

Examples:

01 (Black BG, White Text)

02 (Black BG, Green Text - Default)

18 (White BG, Dark Gray Text)

📂 Tab Breakdown
1. CMD
Fully interactive. Click directly into the terminal to type commands.

Type cls to clear the screen.

Use the floating CLS button on the bottom right of standard terminals.

2. ThisPC
Visual file navigation.

Double-click a folder to enter it; Double-click a file to edit.

Right-click files to Compile (.java), Open (Normal/Braille mode), Rename, or Delete.

Use the top bar to create new folders and files.

3. NetworkChuck
Left-Click: Opens a prompt for required input (like URLs, IPs, or file paths) and runs the command.

Right-Click: Runs the pre-configured example command immediately.

Includes a dynamic Wi-Fi password extractor and various curl utilities.

4. Java (Press Ctrl + Alt + J)
Automatically manages src and bin folders based on your current project directory.

Collapsible project file tree (toggle via ☰).

Save & Run compiles your code and launches it in the built-in terminal.

The terminal unlocks for input when your Java program asks for user input (e.g., Scanner).

5. HTML (Press Ctrl + Alt + H)
Quick generation of index.html, style.css, and script.js.

Automatically injects <link> and <script> tags into your HTML file.

Click Preview to open the current HTML file in your default web browser.

🧠 Technical Highlights
ANSI Stripping: The command prompt wrapper intercepts stdout/stderr and strips ANSI escape codes (\u001B[...m) to prevent terminal garbage text.

Custom Rendering: Braille art is rendered using a custom JPanel and Graphics2D rather than a JTextArea. This ensures perfectly monospaced grid alignment for complex ASCII art.

ANSI Inline Colors: The Braille viewer parses $1 to $9 tags and dynamically changes the Graphics2D color context without disrupting the character grid.

Thread Isolation: File compilation, process execution, and terminal input streams are handled on background threads to prevent the Swing UI from freezing.
