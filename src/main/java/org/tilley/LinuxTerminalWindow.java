package org.tilley;

import org.lwjgl.glfw.GLFW;
import org.rusherhack.client.api.feature.window.ResizeableWindow;
import org.rusherhack.client.api.ui.window.content.component.TextFieldComponent;
import org.rusherhack.client.api.ui.window.content.ComboContent;
import org.rusherhack.client.api.ui.window.view.RichTextView;
import org.rusherhack.client.api.ui.window.view.TabbedView;
import org.rusherhack.client.api.ui.window.view.WindowView;

import java.awt.*;
import java.io.*;
import java.util.List;

public class LinuxTerminalWindow extends ResizeableWindow {
    private final TabbedView rootView;
    private final RichTextView rusherShellView;
    private final TextFieldComponent inputBox;

    private final String username = System.getProperty("user.name");
    private final String startDirectory = System.getProperty("user.home");

    String hostname = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[1];

    String command = null;
    String prompt = updatePrompt();

    private Process bashProcess;
    private BufferedWriter toShell;
    private BufferedReader fromShell;

    public LinuxTerminalWindow() {
        super("Linux Terminal", 200, 100, 400, 300);
        this.rusherShellView = new RichTextView("commands", this);
        final ComboContent inputCombo = new ComboContent(this);
        inputBox = new TextFieldComponent(this, this.getWidth());
        inputBox.setValue(prompt);
        inputCombo.addContent(inputBox, ComboContent.AnchorSide.LEFT);
        this.rootView = new TabbedView(this, List.of(this.rusherShellView, inputCombo));
        startShell();
    }

    private void startShell() {
        try {
            bashProcess = new ProcessBuilder(System.getenv("SHELL")).directory(new File(startDirectory)).redirectErrorStream(true).start();
            toShell = new BufferedWriter(new OutputStreamWriter(bashProcess.getOutputStream()));
            fromShell = new BufferedReader(new InputStreamReader(bashProcess.getInputStream()));

            new Thread(() -> {
                String line;
                try { while ((line = fromShell.readLine()) != null) { if (!line.isEmpty()) addLine(line); }
                } catch (IOException ignored) {}
            }).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addLine(String line) {
        this.rusherShellView.add(line, Color.LIGHT_GRAY.getRGB());
    }

    private String updatePrompt() {
        return "";
    }

    @Override
    public boolean keyTyped(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER) {
            command = inputBox.getValue().substring(prompt.length());

            if (command.equals("clear")) {
                rusherShellView.clear();

            } else {
                addLine(prompt + command);
                try {
                    toShell.write(command + "\n");
                    toShell.flush();
                } catch (IOException ignored) {}
            }

            prompt = updatePrompt();
            inputBox.setValue(prompt);
            return true;

        } else if (key == GLFW.GLFW_KEY_C && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            bashProcess.destroy();
            addLine("^C");
            startShell();
            return true;

        } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (inputBox.getValue().length() <= prompt.length()) return true;
        } else if (key == GLFW.GLFW_KEY_UP) {
            inputBox.setValue(prompt + command);
        }

        return super.keyTyped(key, scanCode, modifiers);
    }

    @Override
    public WindowView getRootView() {
        return this.rootView;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
