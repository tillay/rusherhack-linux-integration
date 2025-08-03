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
    private String workingDirectory = "/home/" + username + "/";
    private final String hostname = commandOutput("hostname");

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
            bashProcess = new ProcessBuilder("/bin/bash").directory(new File(workingDirectory)).redirectErrorStream(true).start();
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

    private String commandOutput(String inputCommand) {
        try {
            Process process = new ProcessBuilder(inputCommand.split(" ")).directory(new File(workingDirectory)).start();
            String output = new String(process.getInputStream().readAllBytes());
            return output.isEmpty() ? "" : output.substring(0, output.length() - 1);
        } catch (Exception e) {
            String err = e.toString();
            if (err.contains("Cannot run program")) return "RusherShell: "+inputCommand+": command not found";
            return err;
        }
    }

    private void addLine(String line) {
        this.rusherShellView.add(line, Color.LIGHT_GRAY.getRGB());
    }

    private String updatePrompt() {
        return "[" + username + "@" + hostname + " " + workingDirectory.replaceAll("/home/" + username, "~").replaceAll("//", "/") + " ]$ ";
    }

    @Override
    public boolean keyTyped(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER) {
            command = inputBox.getValue().substring(prompt.length());

            if (command.equals("clear")) {
                rusherShellView.clear();

            } else if (command.split(" ")[0].equals("cd")) {
                String[] parts = command.split(" ");
                addLine(prompt + command);
                if (parts.length == 1) {
                    workingDirectory = "/home/" + username + "/";
                } else if (parts.length == 2 && new File(workingDirectory + "/" + parts[1]).isDirectory()) {
                    workingDirectory = workingDirectory + "/" + parts[1];
                    workingDirectory = commandOutput("pwd") + "/";
                } else {
                    addLine("cd: no such file or directory: " + parts[1]);
                }

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
