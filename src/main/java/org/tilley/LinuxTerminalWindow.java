package org.tilley;

import org.lwjgl.glfw.GLFW;
import org.rusherhack.client.api.feature.window.ResizeableWindow;
import org.rusherhack.client.api.ui.window.content.component.TextFieldComponent;
import org.rusherhack.client.api.ui.window.content.ComboContent;
import org.rusherhack.client.api.ui.window.view.RichTextView;
import org.rusherhack.client.api.ui.window.view.TabbedView;
import org.rusherhack.client.api.ui.window.view.WindowView;

import java.awt.*;
import java.io.File;
import java.util.List;

public class LinuxTerminalWindow extends ResizeableWindow {
    private final TabbedView rootView;
    private final RichTextView rusherShellView;
    private final TextFieldComponent inputBox;

    private final String username = System.getProperty("user.name");

    private String workingDirectory = "/home/"+username+"/";

    private final String hostname = commandOutput("hostname");

    String command = null;

    String prompt = updatePrompt();

    public LinuxTerminalWindow() {
        super("Linux Terminal", 200, 100, 400, 300);
        this.rusherShellView = new RichTextView("commands", this);
        final ComboContent inputCombo = new ComboContent(this);
        inputBox = new TextFieldComponent(this, this.getWidth());
        inputBox.setValue(prompt);
        inputCombo.addContent(inputBox, ComboContent.AnchorSide.LEFT);
        this.rootView = new TabbedView(this, List.of(this.rusherShellView, inputCombo));
    }

    private String commandOutput(String inputCommand) {
        try {
            Process process = new ProcessBuilder(inputCommand.split(" ")).directory(new File(workingDirectory)).start();
            String output = new String(process.getInputStream().readAllBytes());
            output = output.substring(0, output.length() - 1);
            return output;
        } catch (Exception e) {
            if (e.toString().contains("Cannot run program")) {
                return "RusherShell: "+inputCommand+": command not found";
            } else if (e.toString().contains("out of bounds")) {
                return "RusherShell: "+inputCommand+": arguments invalid";
            } else {
                return e.toString();
            }
        }
    }

    private void addLine(String line) {
        this.rusherShellView.add(line, Color.LIGHT_GRAY.getRGB());
    }

    private String updatePrompt() {
        return "["+username+"@"+hostname+" "+workingDirectory.replaceAll("/home/" + username, "~").replaceAll("//", "/")+" ]$ ";
    }

    @Override
    public boolean keyTyped(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER) { // Enter pressed

            command = inputBox.getValue().substring(prompt.length());

            if (command.equals("clear")) { // Custom clearing of the screen
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

            } else { // Attempt to run the command in the shell
                addLine(prompt + command);
                String output = commandOutput(command);
                for (String line : output.split("\n")) addLine(line);
            }

            prompt = updatePrompt();
            inputBox.setValue(prompt);
            return true;

        } else if (key == GLFW.GLFW_KEY_BACKSPACE) { // Only allow backspace if it won't affect the prompt
            if (inputBox.getValue().length() <= prompt.length()) {
                return true;
            }
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
